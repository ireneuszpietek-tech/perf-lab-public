public class Demo {

    public static void s(int x) {
        Thread thread = new Thread(() -> System.out.println(x));
//        System.out.println(x);

//        thread.start();
    }

//    public void i(String s) {               // instance method
//        System.out.println(s);
//    }
//
//    private void p() {                      // private method
//        System.out.println("p");
//    }
//
//    public Demo() {                         // constructor
//        p();
//    }

    public static void main(String[] args) {
        Demo.s(42);                         // 1: static invocation
//        Demo d = new Demo();                // 2: new + invokespecial <init>
//        d.i("hello");                       // 3: virtual invocation
//        d.p();                              // 4: private, but compiler knows it
    }
}