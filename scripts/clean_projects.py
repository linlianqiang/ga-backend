"""Read only the first four XLSX sheets using OOXML; preserve raw values and provenance."""
import zipfile,xml.etree.ElementTree as E,json,re,hashlib,sys,posixpath
from pathlib import Path
from decimal import Decimal,InvalidOperation
from datetime import datetime,timedelta
from collections import Counter,defaultdict
N={'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
def clean(path):
 z=zipfile.ZipFile(path);digest=hashlib.sha256(Path(path).read_bytes()).hexdigest()
 ss=[''.join(t.text or '' for t in e.findall('.//m:t',N)) for e in E.fromstring(z.read('xl/sharedStrings.xml'))]
 wb=E.fromstring(z.read('xl/workbook.xml'));rels={x.get('Id'):x.get('Target') for x in E.fromstring(z.read('xl/_rels/workbook.xml.rels'))};out=[];issues=[];counts={}
 def issue(sheet,row,field,value,reason):issues.append(dict(sheet=sheet,row=row,field=field,value=value,reason=reason))
 for idx,sheet in enumerate(wb.find('m:sheets',N)[:4]):
  name=sheet.get('name');target=rels[sheet.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id')];root=E.fromstring(z.read(posixpath.normpath('xl/'+target) if not target.startswith('/') else target.lstrip('/')));cells={};rows=set()
  for row in root.findall('m:sheetData/m:row',N):
   for c in row:
    v=c.find('m:v',N);val=ss[int(v.text)] if c.get('t')=='s' and v is not None else ''.join(t.text or '' for t in c.findall('.//m:t',N)) if c.get('t')=='inlineStr' else v.text if v is not None and v.text else ''
    if val.strip():cells[c.get('r')]=val.strip();rows.add(int(row.get('r')))
  # Expand only explicit single-column merges. Shared monetary amounts are NOT duplicated.
  merged={}
  for m in root.findall('m:mergeCells/m:mergeCell',N):
   a,b=m.get('ref').split(':');ca,ra=re.match(r'([A-Z]+)(\d+)',a).groups();cb,rb=re.match(r'([A-Z]+)(\d+)',b).groups()
   if ca==cb:
    for r in range(int(ra),int(rb)+1):merged[f'{ca}{r}']=(a,m.get('ref')); 
  headers={re.sub(r'\d','',k):v for k,v in cells.items() if re.search(r'\d+$',k).group()=='1'}
  mapping={'负责人':'maintenanceLeader','执业人':'platformPractitioner','合同编码':'contractNo','项目名称':'projectName','物业名称':'propertyUnit','区域':'region','负责人/联系方式':'contact','面积m²':'area','金额':'contractAmount','款项结算':'settlement','开始时间':'contractStart','结束时间':'contractEnd','续约跟进人':'renewalOwner','跟进时间':'followDate','跟进情况':'followStatus','合同状态':'contractStatus','是否上传合同':'contractUploadedRaw','现场公示牌同步情况':'postedRaw','现场公示牌已同步':'postedRaw'}
  count=0
  for r in sorted(rows-{1}):
   raw={c:cells.get(f'{c}{r}','') for c in headers};d={};shared={}
   for c,h in headers.items():
    key=mapping.get(h)
    if not key:continue
    address=f'{c}{r}';value=raw[c]
    if address in merged:
     anchor,ran=merged[address]
     if key=='contractAmount':
      shared={'sharedAmountGroup':f'{name}!{ran}','sharedAmountRaw':cells.get(anchor,'')}
      if address!=anchor:value='';issue(name,r,key,cells.get(anchor,''),'合并合同金额仅计入首行，当前行不重复计入')
     elif not value:value=cells.get(anchor,'')
    d[key]=value or None
   if not d.get('projectName'):
    issue(name,r,'projectName',raw,'缺少项目名称，跳过');continue
   for key in ['area','contractAmount']:
    v=d.get(key)
    if not v:continue
    try:
     val=Decimal(v.replace(',',''));assert val.is_finite() and val>=0 and val==val.quantize(Decimal('.01')) and val<Decimal('10000000000000000');d[key]=str(val)
    except (InvalidOperation,AssertionError):d[key]=None;issue(name,r,key,v,'混合文字或无效数值，保留原文，不推断金额/面积')
   for key in ['contractStart','contractEnd','followDate']:
    v=d.get(key)
    if not v:continue
    parsed=None
    if re.fullmatch(r'\d{5}(\.0+)?',v):parsed=(datetime(1899,12,30)+timedelta(days=float(v))).date().isoformat()
    else:
     for fmt in ['%Y-%m-%d','%Y.%m.%d','%Y/%m/%d','%Y%m%d']:
      try:parsed=datetime.strptime(v,fmt).date().isoformat();break
      except ValueError:pass
    d[key]=parsed
    if not parsed:issue(name,r,key,v,'日期不明确，保留原文')
   if d.get('contractStart') and d.get('contractEnd') and d['contractStart']>d['contractEnd']:
    issue(name,r,'contractEnd',d['contractEnd'],'结束早于开始，结构化日期置空');d['contractStart']=d['contractEnd']=None
   d['posted']=True if (d.get('postedRaw') or '').startswith('√') else False if (d.get('postedRaw') or '').startswith(('×','❌')) else None
   d['contractUploaded']=True if (d.get('contractUploadedRaw') or '').startswith('√') else False if (d.get('contractUploadedRaw') or '').startswith(('×','❌')) else None
   d.update(shared);d.update(projectType=['QUANZHOU','QUANZHOU','XIAMEN','PARTNER'][idx],bankProject=idx==1,sourceSheet=name,sourceRow=r,sourceKey=f'{digest}:{idx+1}:{r}',sourceRaw=json.dumps(raw,ensure_ascii=False));out.append(d);count+=1
  counts[name]=count
 duplicates=defaultdict(list)
 for d in out:duplicates[(d['projectName'],d.get('contractNo'),d.get('contractStart'),d.get('contractEnd'))].append(d['sourceKey'])
 report={'sha256':digest,'sheetCounts':counts,'count':len(out),'typeCounts':dict(Counter(d['projectType'] for d in out)),'contractAmountTotal':str(sum((Decimal(d['contractAmount']) for d in out if d.get('contractAmount')),Decimal(0))),'issues':issues,'possibleDuplicates':[v for v in duplicates.values() if len(v)>1]}
 return out,report
if __name__=='__main__':
 out,report=clean(sys.argv[1]);Path('data/projects.json').write_text(json.dumps(out,ensure_ascii=False,indent=2));Path('data/projects-import-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print(json.dumps({k:v for k,v in report.items() if k not in ['issues','possibleDuplicates']},ensure_ascii=False));print('issues',len(report['issues']),'possibleDuplicates',len(report['possibleDuplicates']))
