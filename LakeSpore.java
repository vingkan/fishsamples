package fishsamples;
import fish.*;

public class LakeSpore extends Pathogen {
    
    @Override
    public String getName() {
        return "Lake Spore";
    }
    
    @Override
    public double getInfectivity(AgeGroup ageGroup) {
        return 0.1;
    }
    
    @Override
    public double getToxigenicity(AgeGroup ageGroup) {
        return 2.0;
    }
    
    @Override
    public double getResistance(AgeGroup ageGroup) {
        return 2.0;
    }
    
    @Override
    public int expand(int bacteria){
        double factor = 0.5;
        double growth = factor * Math.sqrt(bacteria);
        return (int)growth;
    }
    
}