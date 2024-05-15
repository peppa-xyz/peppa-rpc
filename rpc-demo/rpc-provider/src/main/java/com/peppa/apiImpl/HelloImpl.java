package com.peppa.apiImpl;

import com.peppa.api.Hello;

public class HelloImpl implements Hello {
    @Override
    public String sayHello(String name) {
        return "hello " + name;
    }
}
