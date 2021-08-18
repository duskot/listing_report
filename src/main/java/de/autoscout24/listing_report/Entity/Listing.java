package de.autoscout24.listing_report.Entity;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Listing {

    @CsvBindByName
    private int id;

    @CsvBindByName
    private String make;

    @CsvBindByName
    private double price;

    @CsvBindByName
    private int mileage;

    @CsvBindByName(column = "seller_type")
    private String sellerType;

    public Listing(){}

    public Listing(int id, String make, float price, int mileage, String sellerType) {
        this.id = id;
        this.make = make;
        this.price = price;
        this.mileage = mileage;
        this.sellerType = sellerType;
    }

}
