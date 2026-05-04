package com.lcyhz.urbanova.service.support;

import com.lcyhz.urbanova.domain.DomainConstants;

import java.time.LocalDate;
import java.time.Period;

public final class UserAgeSupport {
    private UserAgeSupport() {
    }

    public static Integer resolveAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public static String resolveAgeGroup(LocalDate birthDate) {
        Integer age = resolveAge(birthDate);
        if (age == null) {
            return DomainConstants.AgeGroup.UNKNOWN;
        }
        if (age < DomainConstants.MINIMUM_RIDER_AGE) {
            return DomainConstants.AgeGroup.CHILD_UNDER_12;
        }
        if (age < 18) {
            return DomainConstants.AgeGroup.TEEN_12_TO_17;
        }
        if (age < 25) {
            return DomainConstants.AgeGroup.YOUNG_ADULT_18_TO_24;
        }
        if (age < 45) {
            return DomainConstants.AgeGroup.ADULT_25_TO_44;
        }
        return DomainConstants.AgeGroup.ADULT_45_PLUS;
    }

    public static boolean isUnderMinimumRiderAge(LocalDate birthDate) {
        Integer age = resolveAge(birthDate);
        return age != null && age < DomainConstants.MINIMUM_RIDER_AGE;
    }
}
