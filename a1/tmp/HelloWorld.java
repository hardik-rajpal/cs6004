class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!"); 
        HelloWorld h = new HelloWorld();
        HelloWorld f = null;
        System.out.println(f.equals(h));
    }
}