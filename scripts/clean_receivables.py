"""Clean source CSV without guessing missing dates, payments or merged cells."""
import csv,json,re,sys,hashlib
from pathlib import Path
from decimal import Decimal,InvalidOperation
from datetime import datetime
source=Path(sys.argv[1]); digest=hashlib.sha256(source.read_bytes()).hexdigest(); records=[]; issues=[]
def date(v,row,field):
 v=v.strip()
 if not v:return None
 for fmt in ['%Y%m%d','%Y.%m.%d','%Y-%m-%d','%Y/%m/%d']:
  try:return datetime.strptime(v,fmt).date().isoformat()
  except ValueError:pass
 issues.append({'row':row,'field':field,'value':v,'reason':'无法确定日期，保留原文'});return None
def money(v,row,field):
 if not v.strip():return None
 try:
  d=Decimal(v.replace(',','').strip());assert d.is_finite() and d>=0 and d==d.quantize(Decimal('.01'));return str(d)
 except (InvalidOperation,AssertionError):issues.append({'row':row,'field':field,'value':v,'reason':'无效金额，保留原文'});return None
with source.open(encoding='utf-8-sig',newline='') as f:
 for n,r in enumerate(csv.reader(f),1):
  if n<3:continue
  r=[v.strip() for v in r];raw=r[:15]
  if not r[2]:
   if any(r):issues.append({'row':n,'reason':'汇总或无项目名称行，不作为账期导入','raw':raw})
   continue
  d=dict(zip(['owner','name','company','property'],r[1:5]));d.update(amount=money(r[5],n,'amount'),net=money(r[6],n,'net'),quarter=r[7],status=r[9] or '未知',account=r[10],method=r[11],invoice=date(r[12],n,'invoice'),follow=date(r[13],n,'follow'),note=r[14],bad=False,due=None,sourceKey=digest+':'+str(n),sourceRaw=json.dumps(raw,ensure_ascii=False))
  ds=re.findall(r'\d{4}[./-]\d{1,2}[./-]\d{1,2}',r[8]);d['start']=date(ds[0],n,'start') if len(ds)==2 else None;d['end']=date(ds[1],n,'end') if len(ds)==2 else None
  if r[8] and len(ds)!=2:issues.append({'row':n,'field':'servicePeriod','value':r[8],'reason':'无法拆分服务日期'})
  if not d['quarter']:d['quarter']=re.split(r'[（(]|\d{4}',r[8])[0].strip()
  records.append(d)
Path('data/receivables.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
Path('data/import-report.json').write_text(json.dumps({'sha256':digest,'count':len(records),'amountTotal':str(sum((Decimal(x['amount']) for x in records if x['amount']),Decimal(0))),'issues':issues},ensure_ascii=False,indent=2))
print('records',len(records),'issues',len(issues))
