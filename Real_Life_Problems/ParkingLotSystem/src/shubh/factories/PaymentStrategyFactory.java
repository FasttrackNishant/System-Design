package shubh.factories;

import shubh.enums.PricingStrategyType;

public class PaymentStrategyFactory {

    public  static  PricingStrategy get(PricingStrategyType type)
    {
        return switch (type){
            case TIME_BASED -> new TimeBasedPricing();
            case EVENT_BASED -> new EventBasedPricing();
        }
    }
}
