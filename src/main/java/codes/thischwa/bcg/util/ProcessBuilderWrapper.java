package codes.thischwa.bcg.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class ProcessBuilderWrapper {

  private final String[] command;
  private final ByteArrayOutputStream outStd = new ByteArrayOutputStream();
  private final ByteArrayOutputStream outError = new ByteArrayOutputStream();
  @Nullable private File workingDirectory;
  // exit value of the process
  @Getter private int status;
  private boolean redirectToStdout;
  private boolean redirectErrorStream;
  @Setter @Nullable private Map<String, String> environment = null;

  public ProcessBuilderWrapper(String... command) {
    this.command = command;
  }

//  public static void main(String[] args) throws IOException {
//    String systemPath = System.getenv("PATH");
//    //        System.out.println(systemPath);
//
//    ProcessBuilderWrapper pb = new ProcessBuilderWrapper("vdirsyncer", "--version");
//    pb.setEnvironment(Map.of("PATH", systemPath));
//    pb.run();
//    System.out.println("status: " + pb.getStatus());
//    if (pb.hasOutput()) {
//      System.out.println("Output: " + pb.getOutput());
//    }
//    if (pb.hasErrors()) {
//      System.out.println("error: " + pb.getError());
//    }
//  }

  /**
   * Executes a command using {@link ProcessBuilder} and handles the process streams.
   *
   * @throws IOException if an I/O error occurs while it is waiting for the process to exit
   */
  public void run() throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    if (workingDirectory != null) {
      pb.directory(workingDirectory);
    }
    if (environment != null && !environment.isEmpty()) {
      pb.environment().putAll(environment);
    }

    pb.redirectErrorStream(redirectErrorStream);

    Process process = pb.start();
    try (InputStream infoStream = process.getInputStream();
        InputStream errorStream = process.getErrorStream()) {
      if (redirectToStdout) {
        infoStream.transferTo(System.out);
        errorStream.transferTo(System.out);
      } else {
        infoStream.transferTo(this.outStd);
        errorStream.transferTo(this.outError);
      }
      status = process.waitFor();
    } catch (InterruptedException e) {
      throw new IOException("InterruptedException was thrown while 'process#waitTor'.", e);
    }
  }

  public void redirectToStdOut() {
    this.redirectToStdout = true;
  }

  public void redirectErrorStream() {
    this.redirectErrorStream = true;
  }

  public void setWorkingDirectory(String dir) {
    this.workingDirectory = Paths.get(dir).toFile();
  }

  public String getOutput() {
    return (redirectToStdout) ? "n/a" : outStd.toString();
  }

  public String getError() {
    return (redirectToStdout) ? "n/a" : outError.toString();
  }

  public boolean hasErrors() {
    return getStatus() != 0;
  }

  public boolean hasOutput() {
    return !getOutput().isEmpty() && !getOutput().equals("n/a");
  }
}
