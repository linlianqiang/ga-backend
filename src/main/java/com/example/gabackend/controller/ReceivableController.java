package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.model.*;
import com.example.gabackend.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;

@RestController
@RequestMapping({"/api/receivables","/api/bills"})
@Transactional
public class ReceivableController {
 private final com.example.gabackend.service.ProjectLinks links;
 private final ReceivableRepository bills; private final ReceiptRepository receipts;
 public ReceivableController(ReceivableRepository b, ReceiptRepository r,com.example.gabackend.service.ProjectLinks l){bills=b;receipts=r;links=l;}
 private boolean contains(String v,String q){return q==null||q.isBlank()||(v!=null&&v.contains(q));}
 private boolean containsAny(String value,List<String> queries){
  if(queries==null)return true;
  var terms=queries.stream().filter(Objects::nonNull).map(String::trim).filter(q->!q.isEmpty()).toList();
  return terms.isEmpty()||terms.stream().anyMatch(q->contains(value,q));
 }
 private boolean due(Receivable b){
  var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
  return "未开票".equals(b.status)&&(
   (b.end!=null&&b.end.isBefore(today))||
   (b.method!=null&&b.method.contains("先付")&&b.start!=null&&b.end!=null
    &&!today.isBefore(b.start)&&!today.isAfter(b.end)));
 }
 private BigDecimal sum(List<Receivable> bs){return bs.stream().map(b->b.amount==null?BigDecimal.ZERO:b.amount).reduce(BigDecimal.ZERO,BigDecimal::add);}
 @GetMapping public Result<?> list(@RequestParam(name="name",defaultValue="") String name,@RequestParam(name="property",required=false) List<String> property,@RequestParam(name="owner",defaultValue="") String owner,@RequestParam(name="tab",defaultValue="all") String tab,@RequestParam(name="page",defaultValue="1") int page,@RequestParam(name="size",defaultValue="0") int size){
 if(page<1||size<0||size>100||!Set.of("all","unpaid","due").contains(tab))return Result.error(400,"分页或tab参数无效");
 var all=bills.findAll().stream().filter(b->contains(b.name,name)&&containsAny(b.property,property)&&contains(b.owner,owner)&&(!tab.equals("unpaid")||"已开票未回款".equals(b.status))&&(!tab.equals("due")||due(b))).sorted(Comparator.comparing((Receivable b)->b.id).reversed()).toList();
 var summary=Map.of("unpaid",sum(all.stream().filter(b->"已开票未回款".equals(b.status)).toList()),"bad",sum(all.stream().filter(b->Boolean.TRUE.equals(b.bad)).toList()),"due",sum(all.stream().filter(this::due).toList()));
 return Result.success(Map.of("items",size==0?all:all.stream().skip((long)(page-1)*size).limit(size).toList(),"total",all.size(),"page",page,"size",size,"summary",summary)); }
 @GetMapping("/{id}") public Result<?> get(@PathVariable("id") Long id){return bills.findById(id).map(Result::success).orElseGet(()->Result.error(404,"账期不存在"));}
 private boolean valid(Receivable b){return b.name!=null&&!b.name.isBlank()&&b.amount!=null&&money(b.amount)&& (b.net==null||money(b.net))&&Set.of("已回款","已开票","已开票未回款","未开票","已填开收据","未知").contains(b.status==null?"":b.status)&&(b.start==null||b.end==null||!b.start.isAfter(b.end));}
 private boolean money(BigDecimal v){return v.signum()>=0&&v.scale()<=2&&v.precision()-v.scale()<=16;}
 @PostMapping public Result<?> create(@RequestBody Receivable b){if(b.projectId==null)return Result.error(400,"请选择关联项目");links.apply(b);if(!valid(b))return Result.error(400,"项目、金额、状态或服务日期无效");b.id=null;b.sourceKey=null;b.sourceRaw=null;return Result.success(bills.save(b));}
 @PutMapping("/{id}") public Result<?> update(@PathVariable("id") Long id,@RequestBody Receivable b){var old=bills.findById(id);if(old.isEmpty())return Result.error(404,"账期不存在");if(b.projectId!=null)links.apply(b);if(!valid(b))return Result.error(400,"项目、金额、状态或服务日期无效");b.id=id;b.sourceKey=old.get().sourceKey;b.sourceRaw=old.get().sourceRaw;return Result.success(bills.save(b));}
 @DeleteMapping("/{id}") public Result<?> delete(@PathVariable("id") Long id){if(!bills.existsById(id))return Result.error(404,"账期不存在");if(receipts.existsByBill(id))return Result.error(409,"请先删除关联回款记录");bills.deleteById(id);return Result.success(null);}
 @GetMapping("/receipts") public Result<?> receipts(@RequestParam(name="bill",required=false) Long bill){return Result.success(bill==null?receipts.findAll():receipts.findByBill(bill));}
 @GetMapping("/receipts/{id}") public Result<?> receipt(@PathVariable("id") Long id){return receipts.findById(id).map(Result::success).orElseGet(()->Result.error(404,"回款不存在"));}
 private boolean valid(Receipt r){return r.bill!=null&&bills.existsById(r.bill)&&r.date!=null&&r.amount!=null&&money(r.amount)&&r.amount.signum()>0;}
 @PostMapping("/receipts") public Result<?> createReceipt(@RequestBody Receipt r){if(!valid(r))return Result.error(400,"账期、回款日期或金额无效");r.id=null;return Result.success(receipts.save(r));}
 @PutMapping("/receipts/{id}") public Result<?> updateReceipt(@PathVariable("id") Long id,@RequestBody Receipt r){if(!receipts.existsById(id))return Result.error(404,"回款不存在");if(!valid(r))return Result.error(400,"账期、回款日期或金额无效");r.id=id;return Result.success(receipts.save(r));}
 @DeleteMapping("/receipts/{id}") public Result<?> deleteReceipt(@PathVariable("id") Long id){if(!receipts.existsById(id))return Result.error(404,"回款不存在");receipts.deleteById(id);return Result.success(null);}
 @GetMapping("/by-project/{id}") public Result<?> byProject(@PathVariable("id") Long id){var bs=bills.findAll().stream().filter(b->Objects.equals(b.projectId,id)).toList();var ids=bs.stream().map(b->b.id).collect(java.util.stream.Collectors.toSet());return Result.success(Map.of("bills",bs,"receipts",receipts.findAll().stream().filter(r->ids.contains(r.bill)).toList()));}
 private static final Map<String,String> EXPORT_FIELDS=Map.ofEntries(Map.entry("id","ID"),Map.entry("projectId","项目ID"),Map.entry("name","项目"),Map.entry("company","公司"),Map.entry("property","物业"),Map.entry("owner","负责人"),Map.entry("quarter","账期"),Map.entry("status","状态"),Map.entry("account","账户"),Map.entry("method","付款方式"),Map.entry("amount","金额"),Map.entry("net","净额"),Map.entry("start","服务开始"),Map.entry("end","服务结束"),Map.entry("invoice","开票日期"),Map.entry("due","付款到期"),Map.entry("follow","跟进日期"),Map.entry("bad","坏账"),Map.entry("note","备注"));
 private static final List<String> DEFAULT_EXPORT_FIELDS=List.of("name","company","property","owner","quarter","amount","status","start","end","invoice","due","note");
 @GetMapping(value="/export",produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
 public org.springframework.http.ResponseEntity<byte[]> export(
   @RequestParam(name="name",defaultValue="") String name,
   @RequestParam(name="property",required=false) List<String> property,
   @RequestParam(name="owner",defaultValue="") String owner,
   @RequestParam(name="tab",defaultValue="all") String tab,
   @RequestParam(name="fields",required=false) List<String> fields) throws java.io.IOException {
  if(!Set.of("all","unpaid","due").contains(tab)) throw new IllegalArgumentException("tab无效");
  var columns=fields==null?DEFAULT_EXPORT_FIELDS:fields;
  if(columns.isEmpty()||columns.stream().anyMatch(f->f==null||!EXPORT_FIELDS.containsKey(f))||new HashSet<>(columns).size()!=columns.size())
   throw new IllegalArgumentException("导出字段不能为空、重复或包含无效字段");
  var rows=bills.findAll().stream().filter(b->contains(b.name,name)&&containsAny(b.property,property)&&contains(b.owner,owner)&&(!tab.equals("unpaid")||"已开票未回款".equals(b.status))&&(!tab.equals("due")||due(b)))
   .sorted(Comparator.comparing((Receivable b)->b.id).reversed()).toList();
  try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var output=new java.io.ByteArrayOutputStream()) {
   var sheet=workbook.createSheet("台账");
   sheet.setDefaultRowHeightInPoints(24);
   var style=workbook.createCellStyle();
   style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
   style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
   style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
   style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
   style.setTopBorderColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
   style.setBottomBorderColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
   style.setLeftBorderColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
   style.setRightBorderColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
   var moneyStyle=workbook.createCellStyle();moneyStyle.cloneStyleFrom(style);
   moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
   var header=sheet.createRow(0);header.setHeightInPoints(24);
   for(int c=0;c<columns.size();c++) {
    sheet.setColumnWidth(c,60*256);
    var cell=header.createCell(c);cell.setCellStyle(style);
    cell.setCellValue(EXPORT_FIELDS.get(columns.get(c)));
   }
   for(int i=0;i<rows.size();i++) {
    var row=sheet.createRow(i+1);row.setHeightInPoints(24);
    for(int c=0;c<columns.size();c++) {
     var cell=row.createCell(c);cell.setCellStyle(style);
     var r=rows.get(i);
     Object value=switch(columns.get(c)) {
    case "id" -> r.id;
    case "projectId" -> r.projectId;
    case "name" -> r.name;
    case "company" -> r.company;
    case "property" -> r.property;
    case "owner" -> r.owner;
    case "quarter" -> r.quarter;
    case "status" -> r.status;
    case "account" -> r.account;
    case "method" -> r.method;
    case "amount" -> r.amount;
    case "net" -> r.net;
    case "start" -> r.start;
    case "end" -> r.end;
    case "invoice" -> r.invoice;
    case "due" -> r.due;
    case "follow" -> r.follow;
    case "bad" -> r.bad;
    case "note" -> r.note;
      default -> throw new IllegalArgumentException("无效字段");
     };
     if(value instanceof BigDecimal amount){cell.setCellValue(amount.doubleValue());cell.setCellStyle(moneyStyle);}
     else if(value!=null)cell.setCellValue(value.toString());
    }

   }
   var totalRow=sheet.createRow(rows.size()+1);totalRow.setHeightInPoints(24);
   for(int c=0;c<columns.size();c++)totalRow.createCell(c).setCellStyle(style);
   int amountColumn=columns.indexOf("amount");
   if(amountColumn<0){
    totalRow.getCell(0).setCellValue("合计："+sum(rows).setScale(2).toPlainString());
   }else{
    var total=totalRow.getCell(amountColumn);total.setCellStyle(moneyStyle);total.setCellValue(sum(rows).doubleValue());
    if(columns.size()==1){
     var totalStyle=workbook.createCellStyle();totalStyle.cloneStyleFrom(moneyStyle);
     totalStyle.setDataFormat(workbook.createDataFormat().getFormat("\"合计：\"#,##0.00"));total.setCellStyle(totalStyle);
    }else totalRow.getCell(amountColumn==0?1:0).setCellValue("合计");
   }
   workbook.write(output);
   return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=receivables.xlsx").body(output.toByteArray());
  }
 }
}
