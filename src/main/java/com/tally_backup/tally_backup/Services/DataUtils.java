package com.tally_backup.tally_backup.Services;

import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.Field;
import com.tally_backup.tally_backup.Dto.TallyExportConfigDtos.Master;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class DataUtils {
    static Logger logger = LoggerFactory.getLogger(DataUtils.class);

    @Value("classpath:xml-header.txt")
    private Resource headerFile;

    @Value("${csv_file_dump_path}")
    private String CSV_FILE_DUMP_PATH;

    public StringBuilder generateXMLfromYAML(Master master, HashMap<String, String> configTallyXML) {
        try {
            StringBuilder xmlHeader = new StringBuilder(StreamUtils.copyToString(headerFile.getInputStream(), StandardCharsets.UTF_8));
            xmlHeader = this.replaceString(xmlHeader, configTallyXML.get("targetCompany"), "{targetCompany}");
            xmlHeader = this.replaceString(xmlHeader, configTallyXML.get("fromDate"), "{fromDate}");
            xmlHeader = this.replaceString(xmlHeader, configTallyXML.get("toDate"), "{toDate}");
            List<String> lstRoutes = new ArrayList<>();
            lstRoutes.addAll(Arrays.stream(master.collection.split("\\.")).toList());
            String targetCollection = lstRoutes.get(0);
            lstRoutes.set(0, "MyCollection");
            for (int i = 0; i < lstRoutes.size(); i++) {
                String xmlPart = String.format("MyPart%02d", i + 1);
                String xmlLine = String.format("MyLine%02d", i + 1);
                xmlHeader = xmlHeader.append("<PART NAME=\"" + xmlPart + "\"><LINES>" + xmlLine + "</LINES><REPEAT>" + xmlLine + " : " + lstRoutes.get(i) + "</REPEAT><SCROLLED>Vertical</SCROLLED></PART>");
            }

            for (int i = 0; i < lstRoutes.size() - 1; i++) {
                String xmlLine = String.format("MyLine%02d", i + 1);
                String xmlPart = String.format("MyPart%02d", i + 2);
                xmlHeader = xmlHeader.append("<LINE NAME=\"" + xmlLine + "\"><FIELDS>FldBlank</FIELDS><EXPLODE>" + xmlPart + "</EXPLODE></LINE>");
            }

            xmlHeader = xmlHeader.append("<LINE NAME=\"" + String.format("MyLine%02d", lstRoutes.size()) + "\">");
            xmlHeader = xmlHeader.append("<FIELDS>");

            for (int i = 0; i < master.fields.size(); i++) {
                xmlHeader = xmlHeader.append(String.format("Fld%02d", i + 1));
                xmlHeader = xmlHeader.append(",");
            }
            xmlHeader = xmlHeader.deleteCharAt(xmlHeader.length() - 1);
            xmlHeader = xmlHeader.append("</FIELDS></LINE>");

            for (int i = 0; i < master.fields.size(); i++) {
                StringBuilder fieldXML = new StringBuilder("<FIELD NAME=\"" + String.format("Fld%02d", i + 1) + "\">");
                Field iField = master.fields.get(i);
                if (iField.getField().matches("^(\\.\\.)?[a-zA-Z0-9_]+$")) {
                    if (iField.getType().equals("text"))
                        fieldXML.append("<SET>$" + iField.getField() + "</SET>");
                    else if (iField.getType().equals("logical"))
                        fieldXML.append("<SET>if $" + iField.getField() + " then 1 else 0</SET>");
                    else if (iField.getType().equals("date"))
                        fieldXML.append("<SET>if $$IsEmpty:$" + iField.getField() + "  then $$StrByCharCode:241 else $$PyrlYYYYMMDDFormat:$" + iField.getField() + ":\"-\"</SET>");
                    else if (iField.getType().equals("number"))
                        fieldXML.append("<SET>if $$IsEmpty:$" + iField.getField() + " then \"0\" else $$String:$" + iField.getField() + "</SET>");
                    else if (iField.getType().equals("amount"))
                        fieldXML.append("<SET>$$StringFindAndReplace:(if $$IsDebit:$" + iField.getField() + " then -$$NumValue:$" + iField.getField() + " else $$NumValue:$" + iField.getField() + "):\"(-)\":\"-\"</SET>");
                    else if (iField.getType().equals("quantity"))
                        fieldXML.append("<SET>$$StringFindAndReplace:(if $$IsInwards:$" + iField.getField() + " then $$Number:$$String:$" + iField.getField() + ":\"TailUnits\" else -$$Number:$$String:$" + iField.getField() + ":\"TailUnits\"):\"(-)\":\"-\"</SET>");
                    else if (iField.getType().equals("rate"))
                        fieldXML.append("<SET>if $$IsEmpty:$" + iField.getField() + " then 0 else $$Number:$" + iField.getField() + "</SET>");
                    else
                        fieldXML.append("<SET>" + iField.getField() + "</SET>");
                } else
                    fieldXML.append("<SET>" + iField.getField() + "</SET>");

                fieldXML.append("<XMLTAG>" + String.format("F%02d", i + 1) + "</XMLTAG>");
                fieldXML.append("</FIELD>");
                xmlHeader.append(fieldXML);

            }
            xmlHeader.append("<FIELD NAME=\"FldBlank\"><SET>\"\"</SET></FIELD>");
            xmlHeader.append("<COLLECTION NAME=\"MyCollection\"><TYPE>" + targetCollection + "</TYPE>");
            if (null != master.fetch && master.fetch.size() > 0)
                xmlHeader.append("<FETCH>" + String.join(",", master.fetch) + "</FETCH>");

            if (null != master.filters && master.filters.size() > 0) {
                xmlHeader.append("<FILTER>");
                for (int i = 0; i < master.filters.size(); i++) {
                    xmlHeader.append(String.format("Fltr%02d", i + 1));
                    xmlHeader.append(",");
                }
                xmlHeader.deleteCharAt(xmlHeader.length() - 1);
                xmlHeader.append("</FILTER>");
            }
            xmlHeader.append("</COLLECTION>");

            if (null != master.filters && master.filters.size() > 0)
                for (int i = 0; i < master.filters.size(); i++)
                    xmlHeader.append("<SYSTEM TYPE=\"Formulae\" NAME=\"" + String.format("Fltr%02d\"", i + 1) + ">" + master.filters.get(i) + "</SYSTEM>");

            xmlHeader.append("</TDLMESSAGE></TDL></DESC></BODY></ENVELOPE>");
            Iterator<String> it = configTallyXML.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = configTallyXML.get(key);
                this.replaceString(xmlHeader, value, key);
            }

            return xmlHeader;
        } catch (Exception ex) {
            logger.error("Error while generateXMLfromYAML " + ex.getMessage());
            return null;
        }
    }

    public String writeCsv(String fileName, String[] data) {
        fileName = CSV_FILE_DUMP_PATH + fileName + "-"+UUID.randomUUID()+".csv";
        try (FileWriter myWriter = new FileWriter(fileName)) {
            for (String line : data) {
                if (null != line)
                    myWriter.write(line);
            }
             myWriter.close();
            return new File(fileName).getAbsolutePath();
        } catch (IOException e) {
            logger.error("Error writing to the file " + fileName);
            throw new RuntimeException(e);
        } catch (Exception ex) {
            logger.error("Error writing to the file " + fileName + " " + ex.getMessage() + " " + ex.getStackTrace());
            throw new RuntimeException(ex);
        }
    }

    public StringBuilder replaceString(StringBuilder inputString, String replacement, String find) {
        int index = inputString.indexOf(find);
        if (index == -1) {
            return inputString;
        }
        return inputString.replace(index, index + find.length(), replacement);
    }

    public String processTdlOutputManipulation(String txt) {
        if (txt == null || txt.isEmpty()) return "";

        try {
            StringBuilder sb = new StringBuilder(txt);

            // Fast string replacements (non-regex)
            String result = sb.toString()
                    .replace("<ENVELOPE>", "")
                    .replace("</ENVELOPE>", "")
                    .replace("<FLDBLANK></FLDBLANK>", "")
                    .replace("\t", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&tab;", "");

            // Apply regex replacements more efficiently
            result = result.replaceAll("\\s*\\r?\\n", "")             // Remove blank lines or leading spaces
                    .replaceAll("</F\\d+>", "")                // Remove tags like </F01>
                    .replaceAll("<F01>", "\r\n")               // Replace <F01> with newline
                    .replaceAll("<F\\d+>", "\t")               // Replace <Fxx> with tab
                    .replaceAll("\\s+<F", "<F")                // Trim leading spaces before <F
                    .replaceAll("&#\\d+;", "")
                    .replaceAll(" \t", "\t");                // Remove any HTML numeric entities

            return result;

        } catch (Exception e) {
            System.err.println("Error in TdlProcessor.processTdlOutputManipulation(): " + e.getMessage());
            return txt;
        }
    }
}