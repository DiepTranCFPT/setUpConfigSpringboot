package com.example.demo.model.Response;

import lombok.Data;

@Data
public class BookingRespone {
    public long id;
    public String date;
    public String court;
    public String slot;
    public String name;
}