package com.example.springbootlab1.controller;

import com.example.springbootlab1.repository.Coordinates;
import com.example.springbootlab1.repository.Date;
import com.example.springbootlab1.service.*;
import com.example.springbootlab1.repository.Country;
import com.example.springbootlab1.data.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
public class SunriseAndSunsetController {
    public static final String ERROR_MESSAGE_1 = "Please specify a valid path";

    private final CountryRepositoryService countryRepositoryService;
    private final CoordinatesRepositoryService coordinatesRepositoryService;
    private final DateRepositoryService dateRepositoryService;

    public SunriseAndSunsetController(CountryRepositoryService countryRepositoryService,
                                      CoordinatesRepositoryService coordinatesRepositoryService,
                                      DateRepositoryService dateRepositoryService){
        this.countryRepositoryService = countryRepositoryService;
        this.coordinatesRepositoryService = coordinatesRepositoryService;
        this.dateRepositoryService = dateRepositoryService;
    }


    //CREATE
    @PostMapping("/sunInfo/country/{countryName}/coordinates")
    public ResponseEntity<Coordinates> addCoordinates(@PathVariable String countryName,
                                                      @RequestParam(value = "lat", defaultValue = "null") String lat,
                                                      @RequestParam(value = "lng", defaultValue = "null") String lng){
        Country country = countryRepositoryService.findByCountryName(countryName);
        if(country == null){
            country = new Country();
            country.setCountryName(countryName);
            countryRepositoryService.save(country);
        }
        if(Objects.equals(lng, "null") || Objects.equals(lat, "null")){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Coordinates coordinates = new Coordinates();
        coordinates.setLat(lat);
        coordinates.setLng(lng);
        coordinates.setCountry(country);
        coordinatesRepositoryService.save(coordinates);
        return new ResponseEntity<>(coordinates, HttpStatus.CREATED);
    }

    @PostMapping(value = "/**")
    public ResponseEntity<String> defaultPostMethod() {
        return new ResponseEntity<>(ERROR_MESSAGE_1, HttpStatus.BAD_REQUEST);
    }


    //READ
    @GetMapping("/sunInfo")
    public String getSunriseAndSunsetInfo(@RequestParam(value = "lat", defaultValue = "null") String lat,
                                          @RequestParam(value = "lng", defaultValue = "null") String lng,
                                          @RequestParam(value = "date", defaultValue = "null") String date,
                                          @RequestParam(value = "formatted", defaultValue = "null") String formatted){
        String url;
        try {
            url = UrlGenerator.generateNewUrl(lat, lng, date, formatted);
        }
        catch (WrongFormatException w){
            return w.getExceptionMessage();
        }

        //"CREATING" DATE AND COORDINATES
        if(Objects.equals(date, "null") || Objects.equals(date, "today")){
            date = LocalDate.now().toString();
        }
        Coordinates coordinates = null;
        List<Coordinates> coordinatesToCheck = coordinatesRepositoryService.findAll();
        for(Coordinates coordinate : coordinatesToCheck){
            if(Objects.equals(coordinate.getLat(), lat) && Objects.equals(coordinate.getLng(), lng)){
                coordinates = coordinate;
            }
        }
        Date coordinatesDate = dateRepositoryService.findByDate(date); //Ищем дату в репозитории
        if(Objects.equals(coordinates, null)){ //Если координаты не существует
            coordinates = new Coordinates();
            coordinates.setLat(lat);
            coordinates.setLng(lng);
            coordinatesRepositoryService.save(coordinates);

            //Проверяем нужно ли дополнительно сохранять и дату
            if(Objects.equals(coordinatesDate, null)){
                coordinatesDate = new Date();
                coordinatesDate.setCoordinatesDate(date);
            }
            coordinatesDate.getCoordinates().add(coordinates);
            dateRepositoryService.save(coordinatesDate);
            coordinates.getDates().add(coordinatesDate);
        }
        else if(!Objects.equals(coordinatesDate, null) && !coordinates.getDates().contains(coordinatesDate)) {
            //Координата существует и существует дата, то мы проверяем привязвны ли эти данные к друг гругу
            coordinatesDate.getCoordinates().add(coordinates);
            dateRepositoryService.save(coordinatesDate);
            coordinates.getDates().add(coordinatesDate);
            coordinatesRepositoryService.save(coordinates);
        }
        else if(Objects.equals(coordinatesDate, null)){
            coordinatesDate = new Date();
            coordinatesDate.setCoordinatesDate(date);
            coordinatesDate.getCoordinates().add(coordinates);
            dateRepositoryService.save(coordinatesDate);
            coordinates.getDates().add(coordinatesDate);
            coordinatesRepositoryService.save(coordinates);
        }

        return JsonKeyExtractor.getFormattedJsonKeys(APIResponse.getJsonInString(url));
    }

    @GetMapping("/sunInfo/country/{countryName}")
    public String getCoordinatesByCountry(@PathVariable String countryName,
                                          @RequestParam(value = "date", defaultValue = "null") String date){

        StringBuilder results = new StringBuilder("Results(");
        Country country = countryRepositoryService.findByCountryName(countryName);
        if(country == null){
            return "No such country!";
        }
        results.append(country.getCountryName()).append("):\n");

        List<Coordinates> coordinatesList = countryRepositoryService.getCoordinatesByCountryName(countryName);
        for(Coordinates coordinate : coordinatesList){
            String url;
            try {
                url = UrlGenerator.generateNewUrl(coordinate.getLat(), coordinate.getLng(), date, null);
            }
            catch (WrongFormatException w){
                return w.getExceptionMessage();
            }
            results.append(JsonKeyExtractor.getFormattedJsonKeys(APIResponse.getJsonInString(url))).append("\n");
        }
        return results.toString();
    }

    @GetMapping("/allCountriesInfo")
    public StringBuilder getAllCountriesInfo() {
        StringBuilder result = new StringBuilder("Countries info:\n\n");
        List<Country> countryList = countryRepositoryService.findAll();
        for (Country country : countryList){
            result.append("\tcountry_id = ").append(country.getId()).append(", name = ").append(country.getCountryName()).append(":\n");
            Set<Coordinates> coordinatesSet = country.getCoordinates();
            for (Coordinates coordinates : coordinatesSet){
                result.append("\t\tcoordinates_id = ").append(coordinates.getId()).append(", lat(").append(coordinates.getLat()).append("), lng(").append(coordinates.getLng()).append(")\n");
            }
        }
        return result;
    }

    @GetMapping("/historyByDate")
    public StringBuilder getHistoryByDate() {
        StringBuilder result = new StringBuilder("History(date):\n\n");
        List<Date> datesList = dateRepositoryService.findAll();
        for (Date date : datesList){
            result.append("\tdate_id = ").append(date.getId()).append(", date = ").append(date.getCoordinatesDate()).append(":\n");
            Set<Coordinates> coordinatesSet = date.getCoordinates();
            for (Coordinates coordinates : coordinatesSet){
                result.append("\t\tcoordinates_id = ").append(coordinates.getId()).append(", lat = ").append(coordinates.getLat()).append(", lng = ").append(coordinates.getLng()).append("\n");
            }
        }
        return result;
    }

    @GetMapping("/historyByCoordinates")
    public StringBuilder getHistoryByCoordinates() {
        StringBuilder result = new StringBuilder("History(coordinates):\n\n");
        List<Coordinates> coordinatesList = coordinatesRepositoryService.findAll();
        for (Coordinates coordinates : coordinatesList){
            Set<Date> dateSet = coordinates.getDates();
            if(Objects.equals(dateSet.size(), 0)){
                continue;
            }
            result.append("\tcoordinates_id = ").append(coordinates.getId()).append(", lat = ").append(coordinates.getLat()).append(", lng = ").append(coordinates.getLng()).append("\n");
            for (Date date : dateSet){
                result.append("\t\tdate_id = ").append(date.getId()).append(", date = ").append(date.getCoordinatesDate()).append(":\n");
            }
        }
        return result;
    }

    @GetMapping(value = "/**")
    public ResponseEntity<String> defaultGetMethod() {
        return new ResponseEntity<>(ERROR_MESSAGE_1, HttpStatus.BAD_REQUEST);
    }


    //PUT
    @PutMapping("/country/{countryName}/{newCountryName}")
    public String updateCountryName(@PathVariable String countryName, @PathVariable String newCountryName){
        Country country = countryRepositoryService.findByCountryName(countryName);
        Country countryCheck = countryRepositoryService.findByCountryName(newCountryName);
        if(Objects.equals(country, null) || !Objects.equals(countryCheck, null)){
            return "Error in changing country name!";
        }
        else{
            country.setCountryName(newCountryName);
            countryRepositoryService.save(country);
            return "Country name updated successfully!";
        }
    }

    @PutMapping("/coordinates/{coordinatesId}")
    public String updateCountryName(@PathVariable Long coordinatesId,
                                    @RequestParam(value = "lat", defaultValue = "null") String lat,
                                    @RequestParam(value = "lng", defaultValue = "null") String lng){
        Coordinates coordinates = coordinatesRepositoryService.findCoordinatesById(coordinatesId);
        if(Objects.equals(coordinates, null) || Objects.equals(lat, "null") || Objects.equals(lng, "null")){
            return "Nonexistent coordinates or wrong parameters!";
        }
        else{
            coordinates.setLat(lat);
            coordinates.setLng(lng);
            coordinatesRepositoryService.save(coordinates);
            return "Coordinates updated successfully!";
        }
    }

    @PutMapping("/date/{dateId}/{newDateValue}")
    public String updateDateValue(@PathVariable Long dateId, @PathVariable String newDateValue){
        Date dateToUpdate = dateRepositoryService.findDateById(dateId);
        Date dateToCheck = dateRepositoryService.findByDate(newDateValue);
        if(Objects.equals(dateToUpdate, null) || !Objects.equals(dateToCheck, null)){
            return "Error in changing date!";
        }
        else{
            dateToUpdate.setCoordinatesDate(newDateValue);
            dateRepositoryService.save(dateToUpdate);
            return "Country name updated successfully!";
        }
    }

    @PutMapping (value = "/**")
    public ResponseEntity<String> defaultPutMethod() {
        return new ResponseEntity<>(ERROR_MESSAGE_1, HttpStatus.BAD_REQUEST);
    }


    //DELETE
    @DeleteMapping("/sunInfo/country/{countryName}")
    public String deleteCoordinates(@PathVariable String countryName){
        Country country = countryRepositoryService.findByCountryName(countryName);
        if(country == null){
            return "There is no such country!";
        }
        List<Coordinates> coordinates = countryRepositoryService.getCoordinatesByCountryName(countryName);
        coordinatesRepositoryService.deleteAll(coordinates);
        countryRepositoryService.delete(country);
        return "Deleted successfully!";
    }

    @DeleteMapping("/sunInfo/country/{countryName}/coordinates/{coordinatesId}")
    public String deleteCoordinates(@PathVariable String countryName, @PathVariable Long coordinatesId){
        Country country = countryRepositoryService.findByCountryName(countryName);
        if(country == null){
            return "There is no such country!";
        }
        List<Coordinates> coordinates = countryRepositoryService.getCoordinatesByCountryName(countryName);
        for (Coordinates coordinate : coordinates){
            if(coordinate.checkId(coordinatesId)){
                coordinatesRepositoryService.deleteById(coordinatesId);
                return "Deleted successfully!";
            }
        }
        return "No coordinates with such id!";
    }

    @DeleteMapping("/history/date/{dateId}")
    public String deleteByDate(@PathVariable Long dateId){
        Date removableDate = dateRepositoryService.findDateById(dateId);
        if(Objects.equals(removableDate, null)){
            return "No such date id!";
        }
        List<Coordinates> coordinates = dateRepositoryService.getCoordinatesByDateId(dateId);
        for (Coordinates coordinate : coordinates){
            if(coordinate.getDates().size() == 1 && Objects.equals(coordinate.getCountry(), null)){
                removableDate.getCoordinates().remove(coordinate);
                coordinate.getDates().remove(removableDate);
                dateRepositoryService.save(removableDate);
                coordinatesRepositoryService.deleteById(coordinate.getId());
            }
            else{
                removableDate.getCoordinates().remove(coordinate);
                coordinate.getDates().remove(removableDate);
                dateRepositoryService.save(removableDate);
            }
        }
        dateRepositoryService.deleteById(removableDate.getId());
        return "Deleted successfully";
    }

    @DeleteMapping("/history/coordinates/{coordinatesId}")
    public String deleteByCoordinates(@PathVariable Long coordinatesId){
        Coordinates removableCoordinates = coordinatesRepositoryService.findCoordinatesById(coordinatesId);
        if(Objects.equals(removableCoordinates, null)){
            return "No such date id!";
        }
        List<Date> dateList = coordinatesRepositoryService.getDateByCoordinatesId(coordinatesId);
        for (Date date : dateList){
            if(date.getCoordinates().size() == 1){
                removableCoordinates.getDates().remove(date);
                date.getCoordinates().remove(removableCoordinates);
                coordinatesRepositoryService.save(removableCoordinates);
                dateRepositoryService.deleteById(date.getId());
            }
            else{
                removableCoordinates.getDates().remove(date);
                date.getCoordinates().remove(removableCoordinates);
                coordinatesRepositoryService.save(removableCoordinates);
            }
        }
        coordinatesRepositoryService.deleteById(removableCoordinates.getId());
        return "Deleted successfully";
    }

    @DeleteMapping(value = "/**")
    public ResponseEntity<String> defaultDeleteMethod() {
        return new ResponseEntity<>(ERROR_MESSAGE_1, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException() {
        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
