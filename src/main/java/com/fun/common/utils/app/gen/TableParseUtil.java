package com.fun.common.utils.app.gen;

import com.fun.common.exception.CodeGenerateException;
import com.fun.common.utils.StringUtils;
import com.fun.project.app.tool.entity.AppGenTable;
import com.fun.project.app.tool.entity.AppGenTableColumn;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析建表SQL生成代码（model-dao-xml）
 *
 * @author DJun
 * @date 2019/8/10 23:02
 */
public class TableParseUtil {

    public static AppGenTable processTableIntoClassInfo(String tableSql) throws IOException {
        if (tableSql == null || tableSql.trim().length() == 0) {
            throw new CodeGenerateException("Table structure can not be empty.");
        }
        tableSql = tableSql.trim();
        String beforeSql="";
        String afterSql="";
        if (tableSql.contains("UNIQUE")) {
            beforeSql = tableSql.substring(tableSql.indexOf("CREATE"),tableSql.indexOf("UNIQUE")-1);
            afterSql=tableSql.substring(tableSql.indexOf("UNIQUE"));
            if (afterSql.contains("BTREE")){
                afterSql=afterSql.substring(afterSql.indexOf("BTREE")+5);
                tableSql=beforeSql+afterSql;
            }else if(afterSql.contains("btree")){
                afterSql=afterSql.substring(afterSql.indexOf("btree")+5);
                tableSql=beforeSql+afterSql;
            }else {
                throw new CodeGenerateException("Table structure anomaly.");
            }
        }
        if (tableSql.contains("unique")){
            beforeSql = tableSql.substring(tableSql.indexOf("CREATE"),tableSql.indexOf("unique")-1);
            afterSql=tableSql.substring(tableSql.indexOf("unique"));
            if (afterSql.contains("BTREE")){
                afterSql=afterSql.substring(afterSql.indexOf("BTREE")+5);
                tableSql=beforeSql+afterSql;
            }else if(afterSql.contains("btree")){
                afterSql=afterSql.substring(afterSql.indexOf("btree")+5);
                tableSql=beforeSql+afterSql;
            }else {
                throw new CodeGenerateException("Table structure anomaly.");
            }
        }

        // table Name
        String tableName = null;
        if (tableSql.contains("TABLE") && tableSql.contains("(")) {
            tableName = tableSql.substring(tableSql.indexOf("TABLE") + 5, tableSql.indexOf("("));
        } else if (tableSql.contains("table") && tableSql.contains("(")) {
            tableName = tableSql.substring(tableSql.indexOf("table") + 5, tableSql.indexOf("("));
        } else {
            throw new CodeGenerateException("Table structure anomaly.");
        }

        if (tableName.contains("`")) {
            tableName = tableName.substring(tableName.indexOf("`") + 1, tableName.lastIndexOf("`"));
        }

        // class Name
        String className = StringUtils.upperCaseFirst(StringUtils.underlineToCamelCase(tableName));
        // class Comment
        String classComment = "";
        if (tableSql.contains("COMMENT=")) {
            String classCommentTmp = tableSql.substring(tableSql.lastIndexOf("COMMENT=") + 8).trim();
            if (classCommentTmp.contains("'") || classCommentTmp.indexOf("'") != classCommentTmp.lastIndexOf("'")) {
                classCommentTmp = classCommentTmp.substring(classCommentTmp.indexOf("'") + 1, classCommentTmp.lastIndexOf("'"));
            }
            if (classCommentTmp != null && classCommentTmp.trim().length() > 0) {
                classComment = classCommentTmp;
            }
        }

        // PRIMARY Key
        String key = "";
        if (tableSql.contains("KEY")) {
            key = tableSql.substring(tableSql.indexOf("KEY") + 3);
        } else if (tableSql.contains("key")) {
            key = tableSql.substring(tableSql.indexOf("key") + 3);
        } else {
            //throw new CodeGenerateException("Table structure anomaly.");
            key = tableSql.substring(tableSql.indexOf("(") + 1, tableSql.indexOf(","));
            if (key.contains("`")) {
                key = key.substring(key.indexOf("`") + 1, key.lastIndexOf("`"));
            }
        }

        if (key.contains("(")) {
            key = key.substring(key.indexOf("(") + 1, key.lastIndexOf(")"));
        }
        if (key.contains("`")) {
            key = key.substring(key.indexOf("`") + 1, key.lastIndexOf("`"));
        }
        //System.out.println("::::"+key);
        String ConversionKey = StringUtils.toCamelCase(key);
        // field List
        List<AppGenTableColumn> fieldList = new ArrayList<>();
        String fieldListTmp = tableSql.substring(tableSql.indexOf("(") + 1, tableSql.lastIndexOf(")"));
        // replave "," by "，" in comment
        Matcher matcher = Pattern.compile("\\ COMMENT '(.*?)\\'").matcher(fieldListTmp);     // "\\{(.*?)\\}"
        while (matcher.find()) {
            String commentTmp = matcher.group();
            commentTmp = commentTmp.replaceAll("\\ COMMENT '|\\'", "");      // "\\{|\\}"

            if (commentTmp.contains(",")) {
                String commentTmpFinal = commentTmp.replaceAll(",", "，");
                fieldListTmp = fieldListTmp.replace(commentTmp, commentTmpFinal);
            }
        }

        // remove invalid data
        for (Pattern pattern : Arrays.asList(
                Pattern.compile("[\\s]*PRIMARY KEY .*(\\),|\\))"),      // remove PRIMARY KEY
                Pattern.compile("[\\s]*UNIQUE KEY .*(\\),|\\))"),       // remove UNIQUE KEY
                Pattern.compile("[\\s]*KEY .*(\\),|\\))")               // remove KEY
        )) {
            Matcher patternMatcher = pattern.matcher(fieldListTmp);
            while (patternMatcher.find()) {
                fieldListTmp = fieldListTmp.replace(patternMatcher.group(), "");
            }
        }

        String[] fieldLineList = fieldListTmp.split(",");
        if (fieldLineList.length > 0) {
            for (String columnLine : fieldLineList) {
                columnLine = columnLine.trim();                                                // `userid` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                if (columnLine.startsWith("`")) {

                    // column Name
                    columnLine = columnLine.substring(1);                                    // userid` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                    String columnName = columnLine.substring(0, columnLine.indexOf("`"));    // userid

                    // field Name
                    String fieldName = StringUtils.lowerCaseFirst(StringUtils.underlineToCamelCase(columnName));
                    if (fieldName.contains("_")) {
                        fieldName = fieldName.replaceAll("_", "");
                    }

                    // field class
                    columnLine = columnLine.substring(columnLine.indexOf("`") + 1).trim();    // int(11) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                    String fieldClass = Object.class.getSimpleName();
                    if (columnLine.startsWith("int") || columnLine.startsWith("tinyint") || columnLine.startsWith("smallint")) {
                        fieldClass = Integer.TYPE.getSimpleName();
                    } else if (columnLine.startsWith("bigint")) {
                        fieldClass = Long.TYPE.getSimpleName();
                    } else if (columnLine.startsWith("float")) {
                        fieldClass = Float.TYPE.getSimpleName();
                    } else if (columnLine.startsWith("double")) {
                        fieldClass = Double.TYPE.getSimpleName();
                    } else if (columnLine.startsWith("datetime") || columnLine.startsWith("timestamp")) {
                        fieldClass = Date.class.getSimpleName();
                    } else if (columnLine.startsWith("varchar") || columnLine.startsWith("text") || columnLine.startsWith("char")) {
                        fieldClass = String.class.getSimpleName();
                    } else if (columnLine.startsWith("decimal")) {
                        fieldClass = BigDecimal.class.getSimpleName();
                    }

                    // field comment
                    String fieldComment = "";
                    if (columnLine.contains("COMMENT")) {
                        String commentTmp = fieldComment = columnLine.substring(columnLine.indexOf("COMMENT") + 7).trim();    // '用户ID',
                        if (commentTmp.contains("'") || commentTmp.indexOf("'") != commentTmp.lastIndexOf("'")) {
                            commentTmp = commentTmp.substring(commentTmp.indexOf("'") + 1, commentTmp.lastIndexOf("'"));
                        }
                        fieldComment = commentTmp;
                    }

                    AppGenTableColumn fieldInfo = new AppGenTableColumn();
                    fieldInfo.setColumnName(columnName);
                    fieldInfo.setFieldName(fieldName);
                    fieldInfo.setFieldClass(fieldClass);
                    fieldInfo.setFieldComment(fieldComment);

                    fieldList.add(fieldInfo);
                }
            }
        }

        if (fieldList.size() < 1) {
            throw new CodeGenerateException("Table structure anomaly.");
        }

        AppGenTable appGenTable = new AppGenTable();
        appGenTable.setTableName(tableName);
        String updateClassName = "";
        int index = 0;
        if (tableName.contains("_")) {
            String[] strArr = tableName.split("_");
            for (int i = 1; i < strArr.length; ++i) {
                index++;
                if (index == 1) {
                    updateClassName = StringUtils.upperCaseFirst(strArr[i]);
                }
                if (index > 1) {
                    updateClassName = updateClassName + StringUtils.upperCaseFirst(strArr[i]);
                }
            }
        } else {
            updateClassName = className;
        }
        appGenTable.setClassName(updateClassName);
        appGenTable.setClassComment(classComment);
        appGenTable.setFieldList(fieldList);
        appGenTable.setPrimaryKey(key);
        appGenTable.setConversionPrimaryKey(ConversionKey);
        return appGenTable;
    }
}
