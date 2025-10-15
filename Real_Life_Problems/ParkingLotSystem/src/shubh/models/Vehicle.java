package shubh.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shubh.enums.VehicleType;

@Getter
@RequiredArgsConstructor
public abstract class Vehicle {

    private  final  String number;
    private  final VehicleType vehicleType;

}
