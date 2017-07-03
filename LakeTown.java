package fishsamples;
import fish.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

/*
 * F.I.S.H.
 * The Fictional Infection Simulation Host
 */
public class LakeTown {
    
    public static int INFECTION_TIME = 11;
    public static int INTERVENTION_TIME = 24 * 16;
    public static int MAX_TURNS = 5000;
    public static int FAMILY_SIZE = 4; 
    public static boolean SIM_BY_DAY = true;
    public static boolean WRITE_RESULTS = true;

    public static String FILE_COORDS = "files/coords.txt";
    public static String FILE_SIR = "files/sir.txt";
    public static String FILE_GEO = "files/geo.txt";
    public static String FILE_PEOPLE = "files/people.txt";
    public static String FILE_CASES = "files/cases.txt";
    public static String FILE_ENC_CASES = "files/enc_cases.txt";

    public static void main(String[] args){
        
        int seed = 12;
        if(args.length >= 1){
            seed = Integer.parseInt(args[0]);
        }
        Helper.setSeed(seed);
        if(!WRITE_RESULTS){
            Helper.disableWrites();
        }
        
        City city = initCity();
        //initInfection(city);
        runSimulation(city);
        findAllCases(city);
        //findEarlyCases(city);
        
        int total = QuarantineMeasure.getTotalAffected();
        double cost = QuarantineMeasure.getTotalCost();
        NumberFormat mf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        System.out.println("Quarantined " + total + " people at cost of " + mf.format(cost));
        
        //Helper.printPeopleData(FILE_PEOPLE, city);
        System.out.println("Outbreak Length: " + ((double)city.getTime() / 24.0) + " days.");
        Helper.closeAllFiles();
        
    }
    
    /*
     * Populate city with locations, people, and routines
     */
    public static City initCity(){
        City city = new City("Lake Town");
        List<String[]> coords = Helper.readCoordsFromFile(FILE_COORDS);
        for(int lidx = 0; lidx < coords.size(); lidx++){
            String[] pair = coords.get(lidx);
            String id = lidx + "";
            String locName = pair[2];
            double lat = Double.parseDouble(pair[0]);
            double lng = Double.parseDouble(pair[1]);
            Location loc = null;
            try{
                String symbol = locName.split(Pattern.quote("("))[1].split(Pattern.quote(")"))[0];
                switch(symbol){
                    case "R":
                        loc = new Restaurant(id, lat, lng, locName);
                        break;
                    case "G":
                        loc = new Grocery(id, lat, lng, locName);
                        break;
                    case "C":
                        loc = new Company(id, lat, lng, locName);
                        break;
                    case "B":
                        loc = new Beach(id, lat, lng, locName);
                        break;
                    default:
                        break;
                }
            }
            catch(Exception e){
                loc = new Home(id, lat, lng, locName);
                for(int f = 0; f < FAMILY_SIZE; f++){
                    AgeGroup ag = AgeGroup.CHILD;
                    Routine routine = new ChildRoutine();
                    if(f < 2){
                        ag = AgeGroup.ADULT;
                        routine = new AdultRoutine(city, loc);
                    }
                    String pid = "LT" + city.getPeople().size();
                    Person person = new Person(pid, ag, routine, loc);
                    person.addNamedLocation("Home", loc);
                    ControlMeasure control = new QuarantineMeasure(person);
                    person.setControlMeasure(control);
                    city.addPerson(person);
                }
            }
            if(loc != null){
                city.addLocation(loc);   
            }
        }
        Map<Class, List<Location>> locMap = new HashMap<Class, List<Location>>();
        for(Location lc : city.getLocations()){
            Class cls = lc.getClass();
            if(!locMap.containsKey(cls)){
                locMap.put(cls, new ArrayList<Location>());
            }
            locMap.get(cls).add(lc);
        }
        System.out.println("City Overview: " + city.getName());
        System.out.println("Total Locations: " + city.getLocations().size());
        for(Map.Entry<Class, List<Location>> entry : locMap.entrySet()){
            System.out.println(entry.getKey() + ": " + entry.getValue().size());
        }
        System.out.println("Total Population: " + city.getPeople().size());
        for(Map.Entry<AgeGroup, List<Person>> entry : Person.groupPeopleByAgeGroup(city.getPeople()).entrySet()){
            System.out.println(entry.getKey() + ": " + entry.getValue().size());
        }
        return city;
    }
    
    public static class IsolationMeasure extends ControlMeasure {
        
        private Location site;
        
        public IsolationMeasure(Person person){
            super("Isolation");
            this.setStartDay(17);
            this.setEndDay(MAX_TURNS);
            this.site = person.getNamedLocation("Home");
        }
        
        public Location applyMeasure(City city, Person person, Location nextLoc){
            Location res = nextLoc;
            if(person.feelsSick()){
                res = this.site;
            }
            return res;
        }
        
    }
    
    public static class QuarantineMeasure extends ControlMeasure {
        
        private boolean searchedHistory = false;
        private boolean wasQuarantined = false;
        private boolean isQuarantined = false;
        private int timeQuarantined = 0;
        private int quarantineFor = 0;
        private Location site = null;
        private List<String> targetLocations;
        
        public QuarantineMeasure(Person person){
            super("Quarantine");
            this.setStartDay(17);
            this.setEndDay(12 + 19);
            this.quarantineFor = 24 * 10;
            this.targetLocations = new ArrayList<String>();
            this.targetLocations.add("Restaurant 1 (R)");
            this.targetLocations.add("Restaurant 7 (R)");
            this.targetLocations.add("Restaurant 16 (R)");
            this.site = person.getNamedLocation("Home");
        }
        public Location applyMeasure(City city, Person person, Location nextLoc){
            Location res = nextLoc;
            if(isQuarantined){
                if(site != null){
                    res = site;   
                }
                if(timeQuarantined > quarantineFor){
                    res = nextLoc;
                    isQuarantined = false;
                }
                int daysQuarantined = (int) Math.round(timeQuarantined / 24);
                if(daysQuarantined > 4 && !person.feelsSick()){
                    //res = nextLoc;
                    //isQuarantined = false;
                }
                timeQuarantined++;
            }
            else if(!wasQuarantined && person.getState() == Person.State.SUSCEPTIBLE){
                for(String locName : targetLocations){
                    if(nextLoc.getName().equals(locName)){
                        isQuarantined = true;
                        wasQuarantined = true;
                        break;
                    }
                }
                if(!isQuarantined && !searchedHistory){
                    searchedHistory = true;
                    for(String locName : targetLocations){
                        for(Person.Record rec : person.getHistory()){
                            if(rec.getLocation().getName().equals(locName)){
                                isQuarantined = true;
                                wasQuarantined = true;
                                break;
                            }
                        }
                        if(isQuarantined){
                            break;
                        }
                    }
                }
            }
            return res;
        }
    }
    
    public static class AdultRoutine extends Routine {
        private Location home;
        private Location workplace;
        private Location groceryStore;
        public AdultRoutine(City city, Location home){
            this.home = home;
            List<Location> workplaces = new ArrayList<Location>();
            List<Location> groceries = new ArrayList<Location>();
            for(Location loc : city.getLocations()){
                if(loc instanceof Company || loc instanceof Restaurant || loc instanceof Grocery){
                    workplaces.add(loc);
                }
                if(loc instanceof Grocery){
                    groceries.add(loc);
                }
            }
            int randomWork = Helper.getRandom().nextInt(workplaces.size());
            int randomGroc = Helper.getRandom().nextInt(groceries.size());
            this.workplace = workplaces.get(randomWork);
            this.groceryStore = groceries.get(randomGroc);
        }
        public Location getNextLocation(Person person, City city){
            Location res = person.getLocation();
            if(city.getHour() < 9 || city.getHour() > 16){
                res = this.home;
            }
            else if(city.getHour() == 11){
                List<Location> choices = new ArrayList<Location>();
                for(Location loc : city.getLocations()){
                    if(loc instanceof Restaurant){
                        choices.add(loc);
                    }
                }
                int randomRes = Helper.getRandom().nextInt(choices.size());
                res = choices.get(randomRes);
            }
            else{
                res = this.workplace;
            }
            return res;
        }
        public Location getHome(){
            return this.home;
        }
        public Location getWorkplace(){
            return this.workplace;
        }
    }
    
    public static class ChildRoutine extends Routine {
        public ChildRoutine(){
            
        }
        public Location getNextLocation(Person person, City city){
            return person.getLocation();
        }
    }
    
    public static class Company extends Location {
        public Company(String id, double lat, double lng, String name){
            super(id, lat, lng, name);
        }
        public void doInteractions(List<Person> people){
            
        }
    }
    
    public static class Grocery extends Location {
        public Grocery(String id, double lat, double lng, String name){
            super(id, lat, lng, name);
        }
        public void doInteractions(List<Person> people){
            
        }
    }
    
    public static class Beach extends Location {
        public Beach(String id, double lat, double lng, String name){
            super(id, lat, lng, name);
        }
        public void doInteractions(List<Person> people){
            
        }
    }
    
    public static class Restaurant extends Location {
        private double frequencyEatContaminatedItem = 0.54;
        public Restaurant(String id, double lat, double lng, String name){
            super(id, lat, lng, name);
        }
        public void doInteractions(List<Person> people){
            List<Person> contagious = Person.getContagious(people);
            for(Person p : contagious){
                for(int pidx = 0; pidx < 2; pidx++){
                    int rand = Helper.getRandom().nextInt(people.size());
                    Person q = people.get(rand);
                    if(p.getPathogen() != null){
                        q.doExposure(p.getPathogen());
                    }
                }
            }
            if(this.isInfected()){
                int eatContaminatedFood = (int)(frequencyEatContaminatedItem * (double)people.size());
                int wereInfected = 0;
                for(int c = 0; c < eatContaminatedFood; c++){
                    int rand = Helper.getRandom().nextInt(people.size());
                    people.get(rand).doExposure(this.getPathogen());
                    if(people.get(rand).getPathogen() != null){
                        wereInfected++;
                    }
                }
                //System.out.println(wereInfected + "/" + eatContaminatedFood + "/" + people.size() + " people who ate contaminated food were infected.");
            }
        }
    }
    
    public static class Home extends Location {
        public Home(String id, double lat, double lng, String name){
            super(id, lat, lng, name);
        }
        public void doInteractions(List<Person> people){
            
        }
    }
    
    /*
     * Infect a single person
     */
    public static void initInfection(City city){
        Pathogen pathogen = new LakeSpore();
        List<Person> people = city.getPeople();
        /*Map<Location, List<Person>> map = Person.groupPeopleByLocation(people);
        for(Map.Entry<Location, List<Person>> entry : map.entrySet()){
            if(entry.getKey() instanceof Restaurant){
                if(entry.getValue().size() >= 1){
                    entry.getValue().get(0).doInfect(pathogen);
                }
            }
        }*/
        for(Person person : people){
            if(person.getRoutine() instanceof AdultRoutine){
                //person.doInfect(pathogen);
                break;
            }
        }
        for(Location loc : city.getLocations()){
            if(loc instanceof Restaurant){
                loc.doInfect(pathogen);
                System.out.println("Infected started at: " + loc);
                break;
            }
        }
    }
    
    /*
     * Run outbreak to completion
     */
    public static void runSimulation(City city){
        Helper.printCityLine(FILE_SIR, city);
        Helper.printLocationLine(FILE_GEO, city);
        boolean outbreak = true;
        while(outbreak){
            if(city.getTime() % 600 == 0){
                System.out.println("Simulation Day: " + (city.getTime() / 24));
            }
            if(city.getTime() == INFECTION_TIME){
                initInfection(city);
            }
            if(city.getTime() == INTERVENTION_TIME){
                Location source = Location.getLocationByName(city.getLocations(), "Restaurant 1 (R)");
                String disRes = source.doDisinfect() ? "Successfully" : "Unsuccessfully";
                System.out.println(disRes + " disinfected " + source);
            }
            city.doTurn();
            if(SIM_BY_DAY){
                if(city.getTime() % 24 == 0){
                    Helper.printCityLine(FILE_SIR, city);
                    Helper.printLocationLine(FILE_GEO, city);
                }
            }
            else{
                Helper.printCityLine(FILE_SIR, city);
                Helper.printLocationLine(FILE_GEO, city);
            }
            boolean pathogenFound = false;
            for(Person person : city.getPeople()){
                if(person.getPathogen() != null){
                    pathogenFound = true;
                    break;
                }
            }
            if(city.getTime() <= INFECTION_TIME){
                outbreak = true;
            }
            else if(!pathogenFound){
            //if(city.getTime() > MAX_TURNS){
                outbreak = false;
                System.out.println("Pathogen died out.");
            }
            else if(city.getTime() > MAX_TURNS){
                outbreak = false;
                System.out.println("Ran out of turns.");
            }
        }
    }

    /*
     * Gather and report all infected cases
     */    
    public static void findAllCases(City city){
        List<Person> resistants = Person.groupPeopleByState(city.getPeople()).get(Person.State.RESISTANT);
        int c = 0;
        for(Person p : resistants){
            List<Person.Record> history = p.getHistory();
            boolean followsAdultRoutine = (p.getRoutine() instanceof AdultRoutine);
            Location home = null;
            Location work = null;
            if(followsAdultRoutine){
                AdultRoutine routine = (AdultRoutine)p.getRoutine();
                home = routine.getHome();
                work = routine.getWorkplace();    
            }
            else{
                home = p.getLocation();
            }
            List<Location> lunches = new ArrayList<Location>();
            int feltSickOn = -1;
            int feltBetterOn = -1;
            for(Person.Record rec : history){
                if(rec.getState() == Person.State.RESISTANT && feltBetterOn < 0){
                    feltBetterOn = rec.getTime();
                }
                else if(rec.getState() == Person.State.INFECTED && feltSickOn < 0){
                    feltSickOn = rec.getTime();
                }
                else if(rec.getState() == Person.State.SUSCEPTIBLE && (rec.getTime() % 24) == 12){
                    lunches.add(rec.getLocation());
                }
            }
            if(feltBetterOn < 0){
                feltBetterOn = history.get(history.size()-1).getTime() + 1;
            }
            String out = "Case " + c + ",";
                out += p.getAge() + ",";
                out += feltSickOn + ",";
                out += feltBetterOn + ",";
                out += home.getName() + "%";
            if(followsAdultRoutine){
                out += work.getName() + "%";
            }
            else{
                out += "---%";
            }
            for(Location lunch : lunches){
                out += lunch.getName() + ",";
            }
            Helper.writeFileLine(FILE_CASES, out);
            String enc = encodeCase(p, c);
            Helper.writeFileLine(FILE_ENC_CASES, enc);
            c++;
        }
        System.out.println(resistants.size() + " people were infected.");
    }
    
    public static void findEarlyCases(City city){
        /*
         * Gather initial early cases
         * Observe from SIR graph where they fall
         * Find sick cases between hour 26 and hour 32 inclusive
         */
        int[] range = {188, 204};
        List<Person> earlyCases = new ArrayList<Person>();
        for(Person p : city.getPeople()){
            for(int h = range[0]; h <= range[1]; h++){
                Person.Record rec = p.getHistory().get(h);
                if(rec.getState() == Person.State.INFECTED){
                    earlyCases.add(p);
                    break;
                }
            }
        }
        System.out.println("Found " + earlyCases.size() + " early cases.");
        /*
         * Retrace early case steps
         * Observe latent, incubation, and recovery periods from pathogen trials
         * Pathogen became latent in 10 hours, incubated in 15
         * Extend range back by 16 hours to see where exposure may have occurred
         * Couldn't find meaningful links, so go back to the start
         */
        int[] retrace = {0, range[1]};
        int ct = 1;
        for(Person p : earlyCases){
            String visits = "Case " + ct + "," + p.getAge() + ",";
            for(int h = retrace[0]; h <= retrace[1]; h++){
                Person.Record rec = p.getHistory().get(h);
                visits += rec.getLocation().getName() + "/" + rec.getState() + ",";
            }
            Helper.writeFileLine(FILE_CASES, visits);
            ct++;
        }
    }
    
    public static String encodeCase(Person p, int index){
        int age = p.getAge();
        int timeSick = 0;
        String home = "N/A";
        String work = "N/A";
        String visits = "";
        if(p.getRoutine() instanceof AdultRoutine){
            AdultRoutine routine = (AdultRoutine)p.getRoutine();
            String homeName = routine.getHome().getName();
            home = homeName.split(Pattern.quote(" "))[1];
            String workName = routine.getWorkplace().getName();
            work = workName; //workName.split(Pattern.quote(" "))[1];
        }
        for(Person.Record rec : p.getHistory()){
            if(rec.getState() == Person.State.INFECTED){
                timeSick = rec.getTime();
                break;
            }
            if((rec.getTime() % 24) == 12){
                if(visits.length() > 0){
                    visits += ",";
                }
                String locName = rec.getLocation().getName();
                String locNum = locName.split(Pattern.quote(" "))[1];
                visits += locNum;
            }
        }
        String dl = "%";
        String res = index + dl + age + dl + timeSick + dl + home + dl + work + dl + visits;
        return res;
    }

}