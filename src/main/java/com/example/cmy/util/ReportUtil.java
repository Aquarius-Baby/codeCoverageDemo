package com.example.cmy.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.cmy.entity.TaskResultTotal;
import com.example.cmy.service.IGitlabService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommitComparison;
import org.gitlab.api.models.GitlabProject;

public class ReportUtil {

    @Autowired
    ParseHtmlUtil parseHtml;

    @Autowired
    IGitlabService gitlabService;


    public static Integer maxDepth = 10;

    /**
     * 根据 index 页面获取数据，入库
     *
     * @param reportFilePath reportPath + "/report/index.html"
     */
    public void getDelCoverage(String reportFilePath) {
        TaskResultTotal taskResultTotal = new TaskResultTotal();
        if (new File(reportFilePath).exists()) {
            Document documentDelta =
                    parseHtml.parseHtml(reportFilePath);

            Elements diffElementsTotal = documentDelta.select("tfoot > tr");
            if (!diffElementsTotal.isEmpty()) {
                for (Element diffElementsTotalTmp : diffElementsTotal) {
                    System.out.println(String.format("diffElementsTotalTmp is %s", diffElementsTotalTmp.text()));
                }
                String[] taskTotalResult = diffElementsTotal.get(0).text().split(" ");
                for (String taskTotalResultItem : taskTotalResult) {
                    System.out.println(String.format("taskTotalResultItem is %s", taskTotalResultItem));
                }
                String[] taskTotalResult1 = ArrayUtils.removeAllOccurences(taskTotalResult, "Total");
                String[] taskTotalResult2 = ArrayUtils.removeAllOccurences(taskTotalResult1, "of");
                if ("n/a".equals(taskTotalResult2[5])) {
                    taskResultTotal.setBranchesCov(0);
                } else {
                    taskResultTotal.setBranchesCov(Integer.parseInt(taskTotalResult2[5].replaceAll("%", "")));
                }

                taskResultTotal.setMissCxty(Integer.parseInt(taskTotalResult2[6].replaceAll(",", "")));
                taskResultTotal.setCxty(Integer.parseInt(taskTotalResult2[7].replaceAll(",", "")));

                taskResultTotal.setMissLines(Integer.parseInt(taskTotalResult2[8].replaceAll(",", "")));
                taskResultTotal.setLines(Integer.parseInt(taskTotalResult2[9].replaceAll(",", "")));

                taskResultTotal.setMissMethods(Integer.parseInt(taskTotalResult2[10].replaceAll(",", "")));
                taskResultTotal.setMethods(Integer.parseInt(taskTotalResult2[11].replaceAll(",", "")));

                taskResultTotal.setMissClasses(Integer.parseInt(taskTotalResult2[12].replaceAll(",", "")));
                taskResultTotal.setClasses(Integer.parseInt(taskTotalResult2[13].replaceAll(",", "")));

                System.out.println(taskResultTotal.toString());
                System.out.println((float)
                        ((taskResultTotal.getLines() - taskResultTotal.getMissLines())
                                / taskResultTotal.getLines()));
            }
        }
    }

    public void getCommitDiff(
            final String fcClassPath,
            final String pcClassPath,
            final Integer projectId,
            final String fromCommitId,
            final String toCommitId) {
        final JSONObject compare = new JSONObject();
        Set<String> javaModifyFileList = new HashSet<String>();
        String pattern = ".*.java";
        GitlabCommitComparison gitlabCommitComparison =
                gitlabService.getCompare(projectId, fromCommitId, toCommitId);
        gitlabCommitComparison
                .getDiffs()
                .forEach(
                        item -> {
                            if (Pattern.matches(pattern, item.getNewPath())) {
                                javaModifyFileList.add(item.getNewPath());
                                List<Integer> modifyLineNum = new ArrayList<Integer>();
                                String tmpRadom = RandomStringUtils.randomAlphanumeric(32);
                                String tmpDiffFilePath = tmpRadom + ".txt";
                                try {
                                    Files.write(Paths.get(tmpDiffFilePath), item.getDiff().getBytes());
                                } catch (IOException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                try {
                                    modifyLineNum = detailDiffTxt(tmpDiffFilePath);
                                } catch (IOException e2) {
                                    // TODO Auto-generated catch block
                                    e2.printStackTrace();
                                }

                                System.out.println("modifyLineNum {}" + modifyLineNum);

                                System.out.println("item.getNewPath {}" + item.getNewPath());

                                String newFileName =
                                        org.apache.commons.lang3.StringUtils.substringAfter(item.getNewPath(), "src/main/java/");

                                Path classStart = Paths.get(fcClassPath);

                                try (Stream<Path> stream =
                                             Files.find(
                                                     classStart,
                                                     maxDepth,
                                                     (path, attr) ->
                                                             String.valueOf(path)
                                                                     .endsWith(
                                                                             org.apache.commons.lang3.StringUtils.substringBeforeLast(newFileName, ".java")
                                                                                     + ".class"))) {
                                    String joined =
                                            stream.sorted().map(String::valueOf).collect(Collectors.joining("; "));

                                    FileUtils.copyFileToDirectory(new File(joined), new File(pcClassPath));
                                    if (joined != null && !joined.isEmpty()) {
                                        final String reportModifyFilePath =
                                                org.apache.commons.lang3.StringUtils.replace(
                                                        org.apache.commons.lang3.StringUtils.substringBeforeLast(newFileName, "/"), "/", ".")
                                                        + File.separator
                                                        + StringUtils.substringAfterLast(newFileName, "/")
                                                        + ".html";
                                        compare.put(reportModifyFilePath, modifyLineNum);
                                    }
                                } catch (IOException e2) {
                                    // TODO Auto-generated catch block
                                    e2.printStackTrace();
                                }
                            }
                        });
    }


    public List<Integer> detailDiffTxt(String filePath) throws NumberFormatException, IOException {
        List<Integer> modifyLineNum = new ArrayList<Integer>();
        BufferedReader bfr = new BufferedReader(new FileReader(filePath));
        String str = null;
        String patternPlus = "^\\+.*";
        String patternMinus = "^\\-.*";
        String patternStart = "^\\@@.*";
        int lineNumber = 0;
        int startNum = 0;

        while ((str = bfr.readLine()) != null) {
            if (Pattern.matches(patternStart, str)) {
                String[] ljtestTmp = str.split(" ")[2].split(",");
                startNum = Integer.valueOf(org.apache.commons.lang.StringUtils.substring(ljtestTmp[0].toString(), 1));
                lineNumber = 0;
            }
            if (Pattern.matches(patternPlus, str)) {
                modifyLineNum.add(lineNumber + startNum - 1);
                lineNumber++;
            } else if (Pattern.matches(patternMinus, str)) {
                System.out.println("{} minus match {}" + lineNumber + " " + str);
            } else {
                System.out.println("{} not match {}" + lineNumber + " " + str);
                lineNumber++;
            }

        }
        bfr.close();
        return modifyLineNum;
    }


    /**
     * 增量-遍历修改页面
     *
     * @param modifyInfoStr
     */
    public void modify(String modifyInfoStr) {
        String reportPath = "";
        // 修改样式
        if (modifyInfoStr != null) {
            final ConcurrentHashMap<String, JSONArray> tmp =
                    new ConcurrentHashMap<String, JSONArray>(16);
            final ConcurrentHashMap<String, List<Integer>> modifyInfo =
                    JSONObject.parseObject(modifyInfoStr, ConcurrentHashMap.class);
            for (final Entry<String, List<Integer>> entry : modifyInfo.entrySet()) {

                try {
                    JSONObject coverInfo =
                            newdetailSourceHtml(
                                    reportPath + "/report/" + entry.getKey(), entry.getValue());
                    final String modify = entry.getKey();
                    final String packageName = modify.substring(0, modify.indexOf("/"));
                    final String javaFile =
                            modify
                                    .substring(modify.indexOf("/") + 1, modify.indexOf(".java.html"))
                                    .concat(".html");
                    if (!tmp.containsKey(packageName)) {
                        tmp.put(packageName, new JSONArray());
                    }
                    JSONObject a = new JSONObject();

                    a.put("sub", javaFile);
                    a.put("cover", coverInfo);
                    tmp.get(packageName).add(a);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            JSONArray p = new JSONArray();
            for (final Entry<String, JSONArray> entry : tmp.entrySet()) {
                try {
                    JSONObject coverInfo =
                            realCover(
                                    reportPath + "/coveragereport/" + entry.getKey() + "/index.html",
                                    entry.getValue());
                    JSONObject a = new JSONObject();
                    a.put("sub", entry.getKey() + "/index.html");
                    a.put("cover", coverInfo);
                    p.add(a);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (p.size() > 0) {
                try {
                    realCover(
                            reportPath + "/coveragereport/" + "index.html", p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 增量覆盖率报告，修改页面 + 样式
     *
     * @param filePath
     * @param modifyLineNum
     * @return
     * @throws IOException
     */
    public JSONObject newdetailSourceHtml(String filePath, List<Integer> modifyLineNum) throws IOException {
        System.out.println("newdetailSourceHtml file path: {}" + filePath);
        JSONObject res = new JSONObject();
        final StringBuffer inputBuffer = new StringBuffer();

        final BufferedReader bfr = new BufferedReader(new FileReader(filePath));
        String str = null;

        int startNum = 1;
        int deltaNc = 0;
        final int totalNum = modifyLineNum.size();

        while ((str = bfr.readLine()) != null) {
            if (modifyLineNum.contains(startNum)) {
                if (str.contains("id=\"L" + startNum + "\"")) {
                    if (str.contains("<span class=\"nc\"")) {
                        deltaNc++;
                    }
                    final String pattern = "class=\"([^\"]+)\"";
                    final Pattern re = Pattern.compile(pattern);
                    final Matcher m = re.matcher(str);
                    if (m.find()) {
                        final String className = m.group(1);
                        str = str.replace("class=\"" + className + "\"", "class=\"" + className + "-diff\"");
                    } else {
                        str = str.replace("id=\"L" + startNum + "\">", "id=\"L" + startNum + "\">+");
                    }

                } else {
                    str = "+ 新修改  " + str;
                }
            }
            inputBuffer.append(str);
            inputBuffer.append('\n');
            startNum++;
        }
        FileUtils.writeStringToFile(new File(filePath), inputBuffer.toString(), "utf-8");
        bfr.close();
        res.put("total", totalNum);
        res.put("miss", deltaNc);
        return res;
    }

    /**
     * 修改页面的样式，统计缺失行数
     *
     * @param filePath
     * @param subName
     * @return
     * @throws IOException
     */
    public JSONObject realCover(final String filePath, final JSONArray subName) throws IOException {
        int total = 0;
        int nc = 0;
        JSONObject coverInfo = new JSONObject();
        Document documentTotal = parseHtml.parseHtml(filePath);
        Element coverageEl = documentTotal.getElementById("coveragetable");
        Element theadEl = coverageEl.select("thead").first();
        Element tbodyEl = coverageEl.select("tbody").first();
        Element tfootEl = coverageEl.select("tfoot").first();

        Element headerEl = theadEl.getElementById("a");
        headerEl.after("<td class=\"sortable\" onclick=\"toggleSort(this)\" id=\"rl\">real Lines</td>");
        headerEl.after("<td class=\"sortable\" onclick=\"toggleSort(this)\" id=\"rm\">real Missed Lines</td>");

        Element totalEl = tfootEl.select("td").get(0);

        for (int i = 0; i < subName.size(); i++) {
            JSONObject item = subName.getJSONObject(i);
            final String sub = ((JSONObject) item).getString("sub");
            final JSONObject cover = ((JSONObject) item).getJSONObject("cover");

            Elements a = tbodyEl.select("a[href*=\"" + sub + "\"]");
            if (a != null) {
                Element b = a.first().parent();

                int t = cover.getIntValue("total");
                int m = cover.getIntValue("miss");
                total += t;
                nc += m;
                b.after("<td class=\"ctr2\">" + t + "</td>");
                b.after("<td class=\"ctr1\">" + m + "</td>");
            }
        }

        totalEl.after("<td class=\"ctr2\">" + total + "</td>");
        totalEl.after("<td class=\"ctr1\">" + nc + "</td>");

        FileUtils.writeStringToFile(new File(filePath), documentTotal.html(), "utf-8");

        coverInfo.put("total", total);
        coverInfo.put("miss", nc);
        return coverInfo;
    }

    public static void main(String[] args) {
        String reportFilePath = "D:/codespace/idea/testDemo/report/index.html";
        TaskResultTotal taskResultTotal = new TaskResultTotal();
        ParseHtmlUtil parseHtml = new ParseHtmlUtil();
        if (new File(reportFilePath).exists()) {
            Document documentDelta =
                    parseHtml.parseHtml(reportFilePath);

            Elements diffElementsTotal = documentDelta.select("tfoot > tr");
            if (!diffElementsTotal.isEmpty()) {
                for (Element diffElementsTotalTmp : diffElementsTotal) {
                    System.out.println(String.format("diffElementsTotalTmp is %s", diffElementsTotalTmp.text()));
                }
                String[] taskTotalResult = diffElementsTotal.get(0).text().split(" ");
                for (String taskTotalResultItem : taskTotalResult) {
                    System.out.println(String.format("taskTotalResultItem is %s", taskTotalResultItem));
                }
                String[] taskTotalResult1 = ArrayUtils.removeAllOccurences(taskTotalResult, "Total");
                String[] taskTotalResult2 = ArrayUtils.removeAllOccurences(taskTotalResult1, "of");
                if ("n/a".equals(taskTotalResult2[5])) {
                    taskResultTotal.setBranchesCov(0);
                } else {
                    taskResultTotal.setBranchesCov(Integer.parseInt(taskTotalResult2[5].replaceAll("%", "")));
                }

                taskResultTotal.setMissCxty(Integer.parseInt(taskTotalResult2[6].replaceAll(",", "")));
                taskResultTotal.setCxty(Integer.parseInt(taskTotalResult2[7].replaceAll(",", "")));

                taskResultTotal.setMissLines(Integer.parseInt(taskTotalResult2[8].replaceAll(",", "")));
                taskResultTotal.setLines(Integer.parseInt(taskTotalResult2[9].replaceAll(",", "")));

                taskResultTotal.setMissMethods(Integer.parseInt(taskTotalResult2[10].replaceAll(",", "")));
                taskResultTotal.setMethods(Integer.parseInt(taskTotalResult2[11].replaceAll(",", "")));

                taskResultTotal.setMissClasses(Integer.parseInt(taskTotalResult2[12].replaceAll(",", "")));
                taskResultTotal.setClasses(Integer.parseInt(taskTotalResult2[13].replaceAll(",", "")));

                System.out.println(taskResultTotal.toString());
                System.out.println((float)
                        ((taskResultTotal.getLines() - taskResultTotal.getMissLines())
                                / taskResultTotal.getLines()));
            }
        } else {
            System.out.println("文件不存在");
        }
    }
}