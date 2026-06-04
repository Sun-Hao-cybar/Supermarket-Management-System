package com.supermarket.backend.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelUtil {

    public static List<Map<String, Object>> importExcel(InputStream inputStream, String[] headers) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        
        int rowCount = sheet.getPhysicalNumberOfRows();
        for (int i = 1; i < rowCount; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Map<String, Object> rowData = new HashMap<>();
            for (int j = 0; j < headers.length; j++) {
                Cell cell = row.getCell(j);
                rowData.put(headers[j], getCellValue(cell));
            }
            dataList.add(rowData);
        }
        workbook.close();
        return dataList;
    }

    private static Object getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue();
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    return (long) value;
                }
                return value;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return cell.getNumericCellValue();
                }
            default:
                return null;
        }
    }

    public static byte[] exportExcel(String[] headers, String[] fieldNames, List<?> dataList) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("数据");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < dataList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Object obj = dataList.get(i);
            for (int j = 0; j < fieldNames.length; j++) {
                Cell cell = row.createCell(j);
                Object value = getFieldValue(obj, fieldNames[j]);
                if (value != null) {
                    if (value instanceof java.math.BigDecimal) {
                        cell.setCellValue(((java.math.BigDecimal) value).doubleValue());
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
}