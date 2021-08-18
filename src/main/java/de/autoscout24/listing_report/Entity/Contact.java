package de.autoscout24.listing_report.Entity;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Contact {

    @CsvBindByName(column = "listing_id")
    private int listingId;

    @CsvBindByName(column = "contact_date")
    private long contactDate;

    public Contact(){}

    public Contact(int listingId, long contactDate) {
        this.listingId = listingId;
        this.contactDate = contactDate;
    }
}
