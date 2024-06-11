package ma.globalperformance.batch;

import ma.globalperformance.entity.Remuneration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelWriter {
    public void writeExcel(List<Remuneration> remunerations, String fileName) {
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Remunerations");
        Row headerRow = sheet.createRow(0);

        String[] columns = {"id", "code_es", "montant", "commission", "trasaction_type", "created_at", "code_oper", "code_service"};

        for (int i = 0; i < columns.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (Remuneration remuneration: remunerations) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(remuneration.getId());
            row.createCell(1).setCellValue(remuneration.getCodeEs());
            row.createCell(2).setCellValue((RichTextString) remuneration.getMontant());
            row.createCell(3).setCellValue((RichTextString) remuneration.getCommission());
            row.createCell(4).setCellValue(remuneration.getTrasactionType());
            row.createCell(5).setCellValue(remuneration.getCreatedAt());
            row.createCell(6).setCellValue(remuneration.getCodeOper());
            row.createCell(7).setCellValue(remuneration.getCodeService());
        }

        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}