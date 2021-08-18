package de.autoscout24.listing_report.Controller;


import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import de.autoscout24.listing_report.Entity.Contact;
import de.autoscout24.listing_report.Entity.Listing;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class UploadReportController {

    CsvToBean<?> csvToBean;
    List<Listing> listings = new ArrayList<>();
    List<Contact> contacts = new ArrayList<>();

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/make-report")
    public String uploadCSVFile(@RequestParam("file") MultipartFile[] files, Model model) {

        int i = 0;

        // validation of number of files and file names
        if (files.length == 2 && (files[0].getOriginalFilename().equals("listings.csv") || files[0].getOriginalFilename().equals("contacts.csv"))
                && (files[1].getOriginalFilename().equals("listings.csv") || files[1].getOriginalFilename().equals("contacts.csv"))) {

            // loop through uploaded files and pars CSV files to create appropriate lists of objects
            for (MultipartFile file : files) {

                // parse CSV file to create lists of Listing and Contact objects
                try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

                    // creating a list of Listing objects
                    if (file.getOriginalFilename().equals("listings.csv")) {
                        // create csv bean reader
                        csvToBean = new CsvToBeanBuilder(reader)
                                .withType(Listing.class)
                                .withIgnoreLeadingWhiteSpace(true)
                                .build();
                        listings = (List<Listing>) csvToBean.parse();
                    } 
                    // creating a list of Contact objects 
                    else {
                        // create csv bean reader
                        csvToBean = new CsvToBeanBuilder(reader)
                                .withType(Contact.class)
                                .withIgnoreLeadingWhiteSpace(true)
                                .build();
                        contacts = (List<Contact>) csvToBean.parse();
                    }
                } catch (Exception ex) {
                    /*
                    * Next exceptions should be considered for validation file structure and content:
                    * CsvDataTypeMismatchException, ConversionException, NumberFormatException
                    * CsvRequiredFieldEmptyException, IllegalStateException
                    * */
                    model.addAttribute("message", "An error occurred while processing the CSV report files.");
                    model.addAttribute("status", false);
                }
            }
        } 
        // save message and status for unsuccessful upload to model
        else {
            model.addAttribute("message", "Please select proper CSV report files to upload.");
            model.addAttribute("status", false);
        }

        // Methods calls for creating reports

        // Report 1: Average Listing Selling Price per Seller Type
        Map<String, Double> averagePricesPerSellerType = averagePricePerSellerType(listings);
        // save Average Listing Selling Price per Seller Type on model
        model.addAttribute("averagePricesPerSellerType", averagePricesPerSellerType);
        model.addAttribute("status", true);

        // Report 2: Distribution (in percent) of available cars by Make
        Map<String,Double> distributionByMake = percentualDistributionOfAvailableCarByMake(listings);
        // save Distribution (in percent) of available cars by Make on model
        model.addAttribute("distributionByMake", distributionByMake);
        model.addAttribute("status", true);

        // Report 3: Average price of the 30% most contacted listings
        Double averagePriceMostContacted = averagePriceOfMostContactedListings(listings, contacts);
        // save Average price of the 30% most contacted listings on model
        model.addAttribute("averagePriceMostContacted", averagePriceMostContacted);
        model.addAttribute("status", true);

        // Report 4: The Top 5 most contacted listings per Month
        topFiveMostContactedListingsPerMonth(listings, contacts);

        return "reports";
    }


    // HELPER METHODS

    // method which return hash map with average prices per seller types
    public Map<String, Double> averagePricePerSellerType(List<Listing> listings) {

        Map<String, Double> result = new HashMap<>();

        result.put("private", listings.stream()
                .filter(l -> l.getSellerType().equals("private"))
                .collect(Collectors.averagingDouble(l -> l.getPrice())));

        result.put("dealer", listings.stream()
                .filter(l -> l.getSellerType().equals("dealer"))
                .collect(Collectors.averagingDouble(l -> l.getPrice())));

        result.put("other", listings.stream()
                .filter(l -> l.getSellerType().equals("other"))
                .collect(Collectors.averagingDouble(l -> l.getPrice())));

        // calling method for creating JSON file with report data on C drive of local computer
        createJSONFile(result, "AveragePricePerSellerType");

        return result;
    }

    // method which return hash map with percents of distribution of available cars by make
    private Map<String, Double> percentualDistributionOfAvailableCarByMake(List<Listing> listings) {

        Map<String, Double> result = new HashMap<>();

        for (Listing listing : listings) {
            if (result.containsKey(listing.getMake())) {
                result.put(listing.getMake(), result.get(listing.getMake()) + 1);
            } else {
                result.put(listing.getMake(), 1.0);
            }
        }

        result.replaceAll((k, v) -> (double) Math.round((v * 100 / listings.size())));

        // sorting hashmap
        Map<String, Double> resultSorted = result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        // calling method for creating JSON file with report data on C drive of local computer
        createJSONFile(resultSorted, "PercentualDistributionOfAvailableCarByMake");

        return resultSorted;

    }
    // method which return value os average price of most contacted listings
    private Double averagePriceOfMostContactedListings(List<Listing> listings, List<Contact> contacts) {

        Map<Integer, Integer> result = new HashMap<>();

        for (Contact contact : contacts) {
            if (result.containsKey(contact.getListingId())) {
                result.put(contact.getListingId(), result.get(contact.getListingId()) + 1);
            } else {
                result.put(contact.getListingId(), 1);
            }
        }

        // sorting hashmap
        Map<Integer, Integer> resultSorted = result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(result.size() / 100 * 30)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        Map<Integer, Listing> mapListing = listings.stream()
                .collect(Collectors.toMap(Listing::getId, Function.identity()));

        double sum = 0;
        for (Integer key : resultSorted.keySet()) {
            sum += mapListing.get(key).getPrice();
        }

        double averagePrice = sum / resultSorted.size();

        // calling method for creating JSON file with report data on C drive of local computer
        createJSONFile(averagePrice, "AveragePriceOfMostContactedListings");

        return averagePrice;
    }
    // method which should return hash map with top five most contacted listings per month
    private void topFiveMostContactedListingsPerMonth(List<Listing> listings, List<Contact> contacts) {

        /*Map<String, Map<Integer, Integer>> mapTopFiveContactedPerMonths = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat("MM.yyyy");
        String dateString;
        for (Contact contact : contacts) {


            //key
            dateString = format.format(new Date(new Timestamp(contact.getContactDate()).getTime()));
            if(mapTopFiveContactedPerMonths.get(dateString).containsKey(contact.getListingId())) {
                mapTopFiveContactedPerMonths.get(dateString).put(contact.getListingId(), mapTopFiveContactedPerMonths.get(dateString).put(contact.getListingId(), mapTopFiveContactedPerMonths.get(dateString).get(contact.getListingId()) + 1));
            } else {
                mapTopFiveContactedPerMonths.put(dateString, (Map<Integer, Integer>) new HashMap<>().put(contact.getListingId(), 1));
            }
        }*/
    }

    // methode for creating JSON file
    private void createJSONFile(Map<String, Double> inputResult, String fileName) {
        // JSON endpoint
        // creating JSON object
        JSONObject jsonObject = new JSONObject();

        // insert map into JSON object
        jsonObject.putAll(inputResult);

        // save the JSON file
        try {
            FileWriter file = new FileWriter("C:/" + fileName + ".json");
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // methode for creating JSON file with another arguments type
    private void createJSONFile(double inputResult, String fileName) {
        // JSON endpoint
        // creating JSON object
        JSONObject jsonObject = new JSONObject();

        // insert map into JSON object
        jsonObject.put("average_price", inputResult);

        // save the JSON file
        try {
            FileWriter file = new FileWriter("C:/" + fileName + ".json");
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
