package fr.uge.loganalyzer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class LogAnalyzer {
  
  private static final Pattern PATTERN = Pattern.compile("\\[([a-zA-Z]*)\\]"); 
  
  private LogAnalyzer(){ 
    super();
  }
  
  /*
  @FunctionalInterface 
  interface Filter { 
    
    boolean accept(String line); // 
    
    private static Filter createFilter(String arg) {
      return switch(arg.toLowerCase()) {
        case "--errors" -> s -> s.contains("[ERROR]");
        case "--warnings" -> s -> s.contains("[WARN]");
        case "--auth" -> s -> s.startsWith("USER_") || s.startsWith("AUTH_");
        case "--all" -> _ -> true;
        default -> throw new IllegalArgumentException("Unknow argument : " + arg);
      }; 
    }
    
    private static Filter or(Filter f1, Filter f2) {
      return s -> f1.accept(s) || f2.accept(s);
    }
    
  }*/
  
  private static String level(String line) {
    var matcher = PATTERN.matcher(line);  
    if(matcher.find()) {
      return matcher.group(1);  
    } else {  
      return "OTHER";
    }
  } 
  
  private static Collector<? super String, ?, ?> createCollector(String arg) { 
    return switch(arg) {
    case "print" ->Collectors.joining("\n");
    case "stats" -> Collectors.groupingBy(LogAnalyzer::level, TreeMap::new, Collectors.counting());
    default -> null; 
    };   
  }
  
  private static Predicate<String> createFilter(String arg) {
    Objects.requireNonNull(arg);
    return switch(arg.toLowerCase()) {
      case "--errors" -> s -> s.contains("[ERROR]");
      case "--warnings" -> s -> s.contains("[WARN]");
      case "--auth" -> s -> s.startsWith("USER_") || s.startsWith("AUTH_");
      case "--all" -> _ -> true; 
      default -> throw new IllegalArgumentException("Unknow argument : " + arg);
    }; 
  }

  private static void processLog(Path filePath, 
      Predicate<String> filter, 
      PrintStream out, 
      Collector<? super String, ?, ?> collector) throws IOException{ 
    Objects.requireNonNull(filePath);
    Objects.requireNonNull(filter);
    Objects.requireNonNull(out);
    /*
    try(var input = Files.newBufferedReader(filePath)) {
      String line;
      while ((line = input.readLine()) != null) { 
        if (filter.test(line)) { 
          out.println(line); 
        }
      }
    } */
    try(var lines = Files.lines(filePath)) { 
      var res = lines.filter(filter).toList();
      /*
      for(var l: res) {
        IO.println(l);
      }*/
      //res.stream().forEach(out::println); 
      out.println(res.stream().collect(collector));
    }
  }

  static void main(String[] args) {
    if (args.length < 2) {
      System.out.print("""
          Usage: java LogAnalyzer <file> <filter-flags>
          Filters:  --all, --errors, --warnings, --auth
          """);
      System.exit(1  );
      return;
    } 
    var file = Path.of(args[0]); 
    var collector = createCollector("print");
    Predicate<String> filter = null;
    try { 
      for (int i=1; i < args.length; i++) {
        var arg = args[i];

        if (arg.startsWith("--")) {
          var newFilter = createFilter(arg);

          if (filter == null) {
            filter =newFilter;
          } else {
            filter = filter.or(newFilter);
          }
        } else {
          collector=createCollector(arg);
        }
      }

      if (filter == null)   {
        filter = _ -> true;
      }

      processLog(file, filter,System.out, collector);
    } catch (IllegalArgumentException e) {
      System.err.println("--unknown argument : " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Cannot read " + file + " : " + e.getMessage());
      System.exit(1);
    }
  }
}
