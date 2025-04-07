package CarAssignment;


import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class InsuranceRatingEngine {

    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Object> knowledgeBase = new HashMap<>();

    public InsuranceRatingEngine() {
        // Initialize knowledge base
        knowledgeBase.put("baseRate.sedan", 1000.0);
        knowledgeBase.put("baseRate.suv", 600.0);
        knowledgeBase.put("baseRate.luxury", 1500.0);
        knowledgeBase.put("baseRate.sports", 1800.0);

        knowledgeBase.put("ageFactor.young", 1000.0);
        knowledgeBase.put("ageFactor.youngAdult", 900.0); // Added young adult factor for 20-24
        knowledgeBase.put("ageFactor.adult", 0.0);
        knowledgeBase.put("ageFactor.senior", 450.0);

        knowledgeBase.put("accidentSurcharge.perAccident", 300.0);

        // Base rate rule
        rules.add(new Rule("base rate",
            profile -> true,
            (profile, premium) -> {
                String category = determineVehicleCategory(profile);
                double baseRate = (double) knowledgeBase.get("baseRate." + category);
                premium.setBaseRate(baseRate);
            }
        ));

        // Age factor rule
        rules.add(new Rule("age factor",
            profile -> true,
            (profile, premium) -> {
                int age = profile.getAge();
                double ageFactor;

                if (age < 20) {
                    ageFactor = (double) knowledgeBase.get("ageFactor.young");
                    premium.addAdjustment("Young Driver Surcharge", ageFactor,
                        "Driver is under 20, higher risk category.");
                } else if (age >= 20 && age < 25) {
                    ageFactor = (double) knowledgeBase.get("ageFactor.youngAdult"); // For drivers 20-24
                    premium.addAdjustment("Young Adult Driver Surcharge", ageFactor,
                        "Driver is between 20 and 24, moderate risk category.");
                } else if (age >= 65) {
                    ageFactor = (double) knowledgeBase.get("ageFactor.senior");
                    premium.addAdjustment("Senior Driver Surcharge", ageFactor,
                        "Driver is 65 or older, senior risk category.");
                } else {
                    ageFactor = (double) knowledgeBase.get("ageFactor.adult");
                    premium.addAdjustment("Adult Driver Adjustment", ageFactor,
                        "Driver is 25-64, standard risk category.");
                }
            }
        ));

        // Accident history rule
        rules.add(new Rule("accident history",
            profile -> profile.getAccidentsInLastFiveYears() > 0,
            (profile, premium) -> {
                int accidents = profile.getAccidentsInLastFiveYears();
                double surchargePerAccident = (double) knowledgeBase.get("accidentSurcharge.perAccident");
                double totalSurcharge = accidents * surchargePerAccident;

                premium.addAdjustment("Accident History Surcharge", totalSurcharge,
                    accidents + " accident(s) in the last five years.");
            }
        ));
    }

    public Premium calculatePremium(DriverProfile profile) {
        Premium premium = new Premium();

        for (Rule rule : rules) {
            if (rule.getCondition().test(profile)) {
                rule.getAction().accept(profile, premium);
            }
        }

        return premium;
    }

    // Determines vehicle category from make/model
    private String determineVehicleCategory(DriverProfile profile) {
        String make = profile.getVehicleMake().toLowerCase();
        String model = profile.getVehicleModel().toLowerCase();

        if (make.contains("lexus") || make.contains("bmw") || make.contains("mercedes")) {
            return "luxury";
        } else if (model.contains("mustang") || model.contains("camaro") || model.contains("corvette")) {
            return "sports";
        } else if (model.contains("crv") || model.contains("rav4") || model.contains("highlander")) {
            return "suv";
        } else {
            return "sedan";
        }
    }

    // Rule class to encapsulate each rule
    private static class Rule {
        private final String name;
        private final Predicate<DriverProfile> condition;
        private final BiConsumer<DriverProfile, Premium> action;

        public Rule(String name, Predicate<DriverProfile> condition, BiConsumer<DriverProfile, Premium> action) {
            this.name = name;
            this.condition = condition;
            this.action = action;
        }

        public Predicate<DriverProfile> getCondition() {
            return condition;
        }

        public BiConsumer<DriverProfile, Premium> getAction() {
            return action;
        }
    }
}
