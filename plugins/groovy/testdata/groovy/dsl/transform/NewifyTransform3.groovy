class Foo {
  Foo(String st){}
}

class Baz {
  Baz(String st){}
}

@Newify(auto=false, value=Foo)
   class Main {
       @Newify() // turn auto on for field
       def field1 = java.math.BigInteger.new(42)
       def field2, field3, field4

       @Newify(Bar)
       def process() {
           field2 = Bar("my bar")
       }

       @Newify(Baz)
       Main() {
           field3 = Foo("my foo")
           field4 = Ba<caret>()
       }
   }