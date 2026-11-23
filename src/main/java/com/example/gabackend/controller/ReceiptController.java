package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.model.Receipt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/receipts") public class ReceiptController {
 private final ReceivableController delegate;
 public ReceiptController(ReceivableController d){delegate=d;}
 @GetMapping public Result<?> list(@RequestParam(name="bill",required=false) Long bill){return delegate.receipts(bill);}
 @GetMapping("/{id}") public Result<?> get(@PathVariable("id") Long id){return delegate.receipt(id);}
 @PostMapping public Result<?> create(@RequestBody Receipt r){return delegate.createReceipt(r);}
 @PutMapping("/{id}") public Result<?> update(@PathVariable("id") Long id,@RequestBody Receipt r){return delegate.updateReceipt(id,r);}
 @DeleteMapping("/{id}") public Result<?> delete(@PathVariable("id") Long id){return delegate.deleteReceipt(id);}
}
