package fishsamples;
import fish.*;

public class PathogenLab {
    
    static String OUTFILE = "files/lab.txt";

    public static void main(String args[]){
        City city = new City("Pathogen City");
        Pathogen pathogen = new LakeSpore();
        Person person = new Person("TEST", AgeGroup.ADULT);
        person.doInfect(pathogen);
        int t = 0;
        int latentOn = -1;
        int incubatedOn = -1;
        int MAX_TURNS = 1000;
        while(person.getState() != Person.State.RESISTANT && t < MAX_TURNS){
            person.doTurn(city);
            if(person.isLatent() && latentOn == -1){
                latentOn = t;
            }
            if(person.isIncubated() && incubatedOn == -1){
                incubatedOn = t;
            }
            String out = "";
                out += t + ",";
                out += person.getState() + ",";
                out += person.getBacteria() + ",";
                out += person.getResponse();
            Helper.writeFileLine(OUTFILE, out);
            t++;
        }
        double DAY = 24.0;
        double latentDays = ((double)latentOn) / DAY;
        double incubatedDays = ((double)incubatedOn) / DAY;
        double totalDays = ((double)t) / DAY;
        System.out.println("Became latent after " + latentDays + " days.");
        System.out.println("Symptoms showing after " + incubatedDays + " days.");
        System.out.println("Infection lasted " + totalDays + " days.");
        Helper.closeAllFiles();
    }

}