public class Smelly {

  private int unused = 42;

  public boolean same(String a, String b) {
    int x = 123456;
    try {
      Integer.parseInt(a);
    } catch (Exception e) {
    }
    return a == b;
  }
}
// modif
