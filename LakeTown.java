package fishsamples;
import fish.*;
import java.util.*;
import java.util.regex.*;

/*
 * F.I.S.H.
 * The Fictional Infection Simulation Host
 */
public class LakeTown {
    
    public static int INFECTION_TIME = 13;
    public static int MAX_TURNS = 1000;
    public static int FAMILY_SIZE = 4; 

    public static String FILE_COORDS = "files/coords.txt";
    public static String FILE_SIR = "files/sir.txt";
    public static String FILE_GEO = "files/geo.txt";
    public static String FILE_PEOPLE = "files/people.txt";
    public static String FILE_CASES = "files/cases.txt";
    public static String FILE_SEED = "files/seed.txt";

    public static void main(String[] args){
        
        Helper.initSeededRandom(FILE_SEED);
        
        City city = initCity();
        //initInfection(city);
        runSimulation(city);
        
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
                    Person person = new Person(ag, routine, loc);
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
        for(Map.Entry<Class, List<Location>> entry : locMap.entrySet()){
            System.out.println(entry.getKey() + ": " + entry.getValue().size());
        }
        return city;
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
            int randomWork = (int)(Helper.nextSeed() * workplaces.size());
            int randomGroc = (int)(Helper.nextSeed() * groceries.size());
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
                int randomRes = (int)(Helper.nextSeed() * choices.size());
                res = choices.get(randomRes);
            }
            else{
                res = this.workplace;
            }
            if(person.feelsSick()){
                res = this.home;
            }
            return res;
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
        private double frequencyEatContaminatedItem = 0.64;
        public Restaurant(String id, double lat, double lng, String name){
            super(id, lat, lng, name);
        }
        public void doInteractions(List<Person> people){
            for(Person p : people){
                for(Person q : people){
                    if(p.getPathogen() != null){
                        q.doExposure(p.getPathogen());
                    }
                }
            }
            if(this.isInfected()){
                int eatContaminatedFood = (int)(frequencyEatContaminatedItem * (double)people.size());
                //System.out.println(eatContaminatedFood + " people ate contaminated food.");
                int wereInfected = 0;
                for(int c = 0; c < eatContaminatedFood; c++){
                    people.get(c).doExposure(this.getPathogen());
                    if(people.get(c).getPathogen() != null){
                        wereInfected++;
                    }
                }
                //System.out.println(wereInfected + " people were infected from food.");
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
            }
        }
    }
    
    /*
     * Run outbreak to completion
     */
    public static void runSimulation(City city){
        Main.printCitySummary(city);
        Helper.printCityLine(FILE_SIR, city);
        Helper.printLocationLine(FILE_GEO, city);
        boolean outbreak = true;
        while(outbreak){
            if(city.getTime() % 120 == 0){
                System.out.println("Simulation Day: " + (city.getTime() / 24));
            }
            if(city.getTime() == INFECTION_TIME){
                initInfection(city);
            }
            city.doTurn();
            //if(city.getTime() % 24 == 0){
                Helper.printCityLine(FILE_SIR, city);
                Helper.printLocationLine(FILE_GEO, city);
            //}
            boolean pathogenFound = false;
            for(Person person : city.getPeople()){
                if(person.getPathogen() != null){
                    pathogenFound = true;
                    break;
                }
            }
            if(!pathogenFound || city.getTime() > MAX_TURNS){
            //if(city.getTime() > MAX_TURNS){
                outbreak = false;
            }
            if(city.getTime() <= INFECTION_TIME){
                outbreak = true;
            }
        }
        
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
        
        
        Helper.printPeopleData(FILE_PEOPLE, city);
        System.out.println("Outbreak Length: " + ((double)city.getTime() / 24.0) + " days.");
    }
    
    public static void printCitySummary(City city){
        List<Person> people = city.getPeople();
        int locations = city.getLocations().size();
        System.out.println(city.getName() + " at Time: " + city.getTime());
        System.out.println(locations + " locations");
        System.out.println(people.size() + " people");
        HashMap<Person.State, List<Person>> stateMap = Person.groupPeopleByState(people);
        for(Person.State state : Person.State.values()){
            String out = state.getName() + ": " + stateMap.get(state).size();
            System.out.println(out);
        }
    }

}