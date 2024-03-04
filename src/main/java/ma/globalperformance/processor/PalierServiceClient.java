package ma.globalperformance.processor;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import ma.globalperformance.dto.PalierDTO;

//@FeignClient(name = "palier-service", url = "${palier.service.url}")
public interface PalierServiceClient {

//    @GetMapping("/paliers/find/oper/{codeOper}/type/{typeTransaction}/es/{codeEspace}")
//    List<PalierDTO> findPaliersByOperTypeEs(@PathVariable String codeOper, @PathVariable String typeTransaction, @PathVariable String codeEspace);
}

