package com.practice.serialzable.java_platform.model;

import java.io.Serializable;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class SerizableModel implements Serializable {

    private SerializablePerson person;

    private UnSerializablePerson unSerializablePersonWithEmptyCons;

    private UnSerizableModel unSerizableModelWithNoEmptyCons;

    public SerizableModel(SerializablePerson person) {
        this.person = person;
    }

    public void setUnSerializablePersonWithEmptyCons(UnSerializablePerson unSerializablePersonWithEmptyCons) {
        this.unSerializablePersonWithEmptyCons = unSerializablePersonWithEmptyCons;
    }

    public void setUnSerizableModelWithNoEmptyCons(UnSerizableModel unSerizableModelWithNoEmptyCons) {
        this.unSerizableModelWithNoEmptyCons = unSerizableModelWithNoEmptyCons;
    }

    @Override
    public String toString() {
        return "SerizableModel{" +
                "person=" + person +
                ", unSerializablePersonWithEmptyCons=" + unSerializablePersonWithEmptyCons +
                ", unSerizableModelWithNoEmptyCons=" + unSerizableModelWithNoEmptyCons +
                '}';
    }
}
