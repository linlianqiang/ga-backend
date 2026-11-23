package com.example.gabackend.controller;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.gabackend.repository.*;
import com.example.gabackend.model.*;
class ReceivableControllerTest {
 @Test void invalidReceiptIsRejected(){var b=mock(ReceivableRepository.class);var r=mock(ReceiptRepository.class);var c=new ReceivableController(b,r,mock(com.example.gabackend.service.ProjectLinks.class));assertEquals(400,c.createReceipt(new Receipt()).getCode());verifyNoInteractions(r);}
 @Test void cannotDeleteBillWithReceipts(){var b=mock(ReceivableRepository.class);var r=mock(ReceiptRepository.class);when(b.existsById(1L)).thenReturn(true);when(r.existsByBill(1L)).thenReturn(true);assertEquals(409,new ReceivableController(b,r,mock(com.example.gabackend.service.ProjectLinks.class)).delete(1L).getCode());verify(b,never()).deleteById(any());}
 @Test void invalidPaginationRejected(){assertEquals(400,new ReceivableController(mock(ReceivableRepository.class),mock(ReceiptRepository.class),mock(com.example.gabackend.service.ProjectLinks.class)).list("",java.util.List.of(),"","all",0,10).getCode());}
 @Test void repeatedPropertyMatchesEitherValueInListAndExport() throws Exception {
  var b=mock(ReceivableRepository.class);
  var first=new Receivable();first.id=1L;first.amount=new java.math.BigDecimal("12.34");first.property="万科物业";
  var second=new Receivable();second.id=2L;second.amount=new java.math.BigDecimal("10.00");second.property="上海悦佳";
  var other=new Receivable();other.id=3L;other.property="其他";
  when(b.findAll()).thenReturn(java.util.List.of(first,second,other));
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new ReceivableController(b,mock(ReceiptRepository.class),mock(com.example.gabackend.service.ProjectLinks.class))).build();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/receivables").param("property","万科","上海悦佳").param("page","1").param("size","20"))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.total").value(2));
  var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/receivables/export").param("property","万科","上海悦佳").param("fields","property,amount"))
   .andReturn().getResponse();
  try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(response.getContentAsByteArray()))) {
   var sheet=workbook.getSheetAt(0);
   assertEquals(4,sheet.getPhysicalNumberOfRows());
   assertEquals("上海悦佳",sheet.getRow(1).getCell(0).getStringCellValue());
   assertEquals("万科物业",sheet.getRow(2).getCell(0).getStringCellValue());
   assertEquals(60*256,sheet.getColumnWidth(0));
   assertEquals(24,sheet.getRow(1).getHeightInPoints());
   assertEquals(org.apache.poi.ss.usermodel.BorderStyle.THIN,sheet.getRow(1).getCell(0).getCellStyle().getBorderBottom());
   assertEquals(2,sheet.getRow(0).getLastCellNum());
   assertEquals("合计",sheet.getRow(3).getCell(0).getStringCellValue());
   assertEquals(22.34,sheet.getRow(3).getCell(1).getNumericCellValue());
   assertEquals(10,sheet.getRow(1).getCell(1).getNumericCellValue());
  }
 }

 @Test void dueUsesServiceEndOrAdvancePaymentForListSummaryAndExport() throws Exception {
  var repo=mock(ReceivableRepository.class);
  var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
  var rows=new java.util.ArrayList<Receivable>();
  for(int i=0;i<11;i++){var row=new Receivable();row.id=(long)i+1;row.status="未开票";row.amount=java.math.BigDecimal.TEN;rows.add(row);}
  rows.get(0).end=today.minusDays(1);
  rows.get(1).start=today;rows.get(1).end=today.plusDays(1);rows.get(1).method="按季度先付";
  rows.get(2).start=today.minusDays(1);rows.get(2).end=today;rows.get(2).method="先付后服务";
  rows.get(3).end=today;
  rows.get(4).due=today.minusDays(1);
  rows.get(5).end=today.minusDays(1);rows.get(5).method="先付";rows.get(5).status="已回款";
  rows.get(6).end=today.plusDays(1);
  rows.get(7).start=today.plusDays(1);rows.get(7).end=today.plusDays(2);rows.get(7).method="先付";
  rows.get(8).end=today.plusDays(1);rows.get(8).method="先付";
  rows.get(9).start=today.minusDays(1);rows.get(9).method="先付";
  rows.get(10).method="先付";
  when(repo.findAll()).thenReturn(rows);
  var c=new ReceivableController(repo,mock(ReceiptRepository.class),mock(com.example.gabackend.service.ProjectLinks.class));
  var data=(java.util.Map<?,?>)c.list("",null,"","due",1,1).getData();
  assertEquals(3,data.get("total"));
  assertEquals(java.util.List.of(rows.get(2)),data.get("items"));
  assertEquals(new java.math.BigDecimal("30"),((java.util.Map<?,?>)data.get("summary")).get("due"));
  var all=(java.util.Map<?,?>)c.list("",null,"","all",1,0).getData();
  assertEquals(new java.math.BigDecimal("30"),((java.util.Map<?,?>)all.get("summary")).get("due"));
  try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(c.export("",null,"","due",java.util.List.of("id")).getBody()))){
   var sheet=workbook.getSheetAt(0);
   assertEquals(5,sheet.getPhysicalNumberOfRows());
   assertEquals("3",sheet.getRow(1).getCell(0).getStringCellValue());
   assertEquals("1",sheet.getRow(3).getCell(0).getStringCellValue());
   assertEquals("合计：30.00",sheet.getRow(4).getCell(0).getStringCellValue());
  }
 }
}
