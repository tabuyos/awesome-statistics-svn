package com.tabuyos.awesome.statistics.svn.start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Description:
 *
 * <pre>
 *   <b>project: </b><i>tabuyos-site</i>
 *   <b>package: </b><i>com.tabuyos.temp.start</i>
 *   <b>class: </b><i>Command</i>
 *   comment here.
 * </pre>
 *
 * @author
 *     <pre><b>username: </b><i><a href="http://www.tabuyos.com">Tabuyos</a></i></pre>
 *     <pre><b>site: </b><i><a href="http://www.tabuyos.com">http://www.tabuyos.com</a></i></pre>
 *     <pre><b>email: </b><i>tabuyos@outlook.com</i></pre>
 *     <pre><b>description: </b><i>
 *   <pre>
 *     Talk is cheap, show me the code.
 *   </pre>
 * </i></pre>
 *
 * @version 0.1.0
 * @since 0.1.0 - 1/15/21 2:46 PM
 */
public class Statistic {

  private static String rawCommand;
  private static String username;
  private static String password;
  private static String user;
  private static String target;
  private static int depth;
  private static String temp;
  private static String base;
  private static String read;
  private static long timeout = 5 * 60 * 1000;
  private static String separator = File.separator;
  private static final StringBuilder builder = new StringBuilder();
  private static boolean diff = false;
  private static String currentTemp;
  private static String debug;

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      help();
      System.exit(0);
    }

    rawCommand = System.getProperty("command");
    username = System.getProperty("username");
    password = System.getProperty("password");
    user = System.getProperty("commit.user");
    target = System.getProperty("svn.target.dir");
    depth = parseInt(System.getProperty("depth"));
    temp = System.getProperty("temp.dir");
    base = System.getProperty("user.dir");
    read = System.getProperty("read.log");
    debug = System.getProperty("debug");
    timeout = parseLong(System.getProperty("timeout"));

    check();
    update();
    List<String> pendingScan = parseScanPath();
    if (read == null) {
      execute(pendingScan);
    }
    compute(pendingScan);
  }

  private static void help() {
    System.out.println("Usage:");
    System.out.println("\tjava [-Doptions] -jar awesome-statistics-svn-xxx.jar");
    System.out.println("Available 'options':");
    System.out.println("\tcommand: \n\t\traw command by user, default null, if you execute this option, then you will execute this command directly.");
    System.out.println("\tusername: \n\t\tusername for user of subversion, require");
    System.out.println("\tpassword: \n\t\tpassword for user of subversion, require");
    System.out.println("\tcommit.user: \n\t\tsearch special user of commit, default username.");
    System.out.println("\tsvn.target.dir: \n\t\tsubversion directory, default current directory.");
    System.out.println("\tdepth: \n\t\tspecify the depth for the search, default 0.");
    System.out.println("\ttemp.dir: \n\t\ttemporary log file directory, default $current/temp");
    System.out.println("\tread.log: \n\t\tread from the previous log file, default false");
    System.out.println("\tdebug: \n\t\tturn on debug mode, default false.");
    System.out.println("\ttimeout: \n\t\tmaximum execution time, default 5 minutes.");
  }

  private static int compute(String tempFile) throws IOException {
    File file = new File(tempFile);
    if (!file.exists()) {
      System.out.println("no log file, please remove -Dread.log option.");
      System.exit(0);
    }
    BufferedReader r = new BufferedReader(new FileReader(tempFile));
    int count = 0;
    String line;
    while ((line = r.readLine()) != null) {
      if (line.startsWith("+") && !line.startsWith("+++") && !isBlank(line.substring(1))) {
        count++;
      }
    }
    r.close();
    return count;
  }

  private static void compute(List<String> pendingScan) throws IOException {
    for (String path : pendingScan) {
      String tempFile = temp + File.separator + path + "temp.log";
      String aim = target + path;
      System.out.println(aim + "\n\ttotal count: " + compute(tempFile));
    }
  }

  private static boolean isBlank(String content) {
    return content.trim().length() == 0 || content.trim().equals("");
  }

  private static List<String> parseScanPath() throws Exception {
    List<String> pendingScan = new ArrayList<>();
    if (target.trim().startsWith("http") || target.trim().startsWith("svn")) {
      separator = "/";
    }
    if (!target.endsWith(separator)) {
      target = target + separator;
    }
    StringJoiner baseJoiner = new StringJoiner(" ");
    baseJoiner.add("svn");
    baseJoiner.add("list");
    baseJoiner.add("--username");
    baseJoiner.add(username);
    baseJoiner.add("--password");
    baseJoiner.add(password);
    pendingScan.add(target);
    for (int i = 0; i < depth; i++) {
      String segment = baseJoiner.toString();
      segment = segment + " ";
      List<String> tempList = new ArrayList<>();
      for (String path : pendingScan) {
        String full = segment + path;
        List<String> execute = execute(full);
        if (path.endsWith(separator)) {
          for (String exec : execute) {
            tempList.add(path + exec);
          }
        } else {
          tempList.add(path);
        }
      }
      pendingScan = tempList.stream().distinct().collect(Collectors.toList());
    }

    final String finalTarget = target;
    pendingScan =
        pendingScan.stream()
            .distinct()
            .filter(path -> path.contains("trunk"))
            .map(path -> path.replaceAll(finalTarget, ""))
            .collect(Collectors.toList());
    if (pendingScan.size() == 0) {
      pendingScan.add("");
    }
    return pendingScan;
  }

  private static void execute(List<String> pendingScan) throws Exception {
    for (String path : pendingScan) {
      StringJoiner joiner = new StringJoiner(" ");
      String tempFile = temp + File.separator + path + "temp.log";
      joiner.add("svn");
      joiner.add("log");
      joiner.add("-q");
      joiner.add("--username");
      joiner.add(username);
      joiner.add("--password");
      joiner.add(password);
      joiner.add("--search");
      joiner.add(user);
      joiner.add("--diff");
      joiner.add(target + path);
      //      can't use redirection or pipe symbol
      //      joiner.add(">");
      //      joiner.add(tempFile);
      File file = new File(tempFile);
      if (!file.getParentFile().exists()) {
        boolean mkdirs = file.getParentFile().mkdirs();
      }
      diff = true;
      currentTemp = tempFile;
      execute(joiner.toString());
    }
  }

  private static void update() throws Exception {
    StringJoiner joiner = new StringJoiner(" ");
    joiner.add("svn");
    joiner.add("update");
    joiner.add("--username");
    joiner.add(username);
    joiner.add("--password");
    joiner.add(password);
    joiner.add(target);
    execute(joiner.toString());
  }

  private static void check() throws Exception {
    if (username == null) {
      System.out.println("必须指定 Subversion 的用户名!");
      help();
      System.exit(1);
    }

    if (password == null) {
      help();
      System.out.println("必须指定 Subversion 的密码!");
      System.exit(1);
    }

    if (target == null) {
      target = base;
    }

    if (temp == null) {
      temp = base + File.separator + "temp";
    }

    if (user == null) {
      user = username;
    }

    if (rawCommand != null) {
      System.out.println(String.join("\n", execute(rawCommand)));
      System.exit(0);
    }
  }

  private static int parseInt(String depth) {
    if (depth == null) {
      return 0;
    }
    return Integer.parseInt(depth);
  }

  private static long parseLong(String depth) {
    if (depth == null) {
      return 0;
    }
    return Long.parseLong(depth);
  }

  private static List<String> execute(String command) throws Exception {
    Process process = Runtime.getRuntime().exec(command);

    Worker worker = new Worker(process);
    worker.start();
    try {
      worker.join(timeout);
      if (worker.exit == null) {
        throw new TimeoutException();
      }
    } catch (InterruptedException | TimeoutException ex) {
      worker.interrupt();
      Thread.currentThread().interrupt();
      throw ex;
    } finally {
      process.destroy();
    }

    String result = builder.toString().trim();
    if (Objects.equals(debug, "true")) {
      System.out.println("============================== command ==============================");
      System.out.println(command);
      System.out.println("============================== result ==============================");
      System.out.println(result);
    }
    List<String> list = Arrays.asList(result.split("\n"));
    builder.delete(0, builder.length());
    return list.stream().map(String::trim).collect(Collectors.toList());
  }

  private static class Worker extends Thread {
    private final Process process;
    private Integer exit;

    private Worker(Process process) {
      this.process = process;
    }

    @Override
    public void run() {
      InputStream errorStream;
      InputStream inputStream;
      try {
        errorStream = process.getErrorStream();
        inputStream = process.getInputStream();
        readStreamInfo(errorStream, inputStream);
        exit = process.waitFor();
        process.destroy();
        if (exit != 0) {
          System.out.println("The child process ended abnormally.");
        }
      } catch (InterruptedException ignore) {
        //
      }
    }
  }

  private static void readStreamInfo(InputStream... inputStreams) {
    ExecutorService executorService = Executors.newFixedThreadPool(inputStreams.length);
    for (InputStream in : inputStreams) {
      executorService.execute(new ProcessResult(in));
    }
    executorService.shutdown();
  }

  private static class ProcessResult implements Runnable {
    private final InputStream in;

    public ProcessResult(InputStream in) {
      this.in = in;
    }

    @Override
    public void run() {
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        FileWriter writer = null;
        if (diff) {
          if (currentTemp == null || currentTemp.length() == 0) {
            throw new RuntimeException("null of current temp file.");
          }
          writer = new FileWriter(currentTemp);
        }
        while ((line = br.readLine()) != null) {
          if (diff && writer != null) {
            writer.write(line + "\n");
          }
          builder.append(line).append("\n");
        }
        if (writer != null) {
          writer.flush();
          writer.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
