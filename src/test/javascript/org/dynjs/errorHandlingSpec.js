describe("calling a function on an undefined property of an object", function(){
  it("should include the function name in the error message", function(){
    try {
      foo = {};
      foo.bar.foobar();
    } catch(e) {
      expect(e.toString()).toMatch(/foobar/);
    }
  });
});

