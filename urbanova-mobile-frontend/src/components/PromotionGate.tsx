import React, { useEffect, useRef } from 'react';
import { Alert } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useQuery } from '@tanstack/react-query';

import { DiscountService } from '@services/api';
import { useAuthStore } from '@store/useAuthStore';
import { getEligibilityHeadline, getUserFacingEligibilityType } from '@utils/promotions';

const PROMOTION_PROMPT_PREFIX = 'urbanova:promotion-prompt:';

interface PromotionGateProps {
  onOpenProfile?: () => void;
}

const PromotionGate: React.FC<PromotionGateProps> = ({ onOpenProfile }) => {
  const user = useAuthStore((state) => state.user);
  const inFlight = useRef(false);

  const eligibilityQuery = useQuery({
    queryKey: ['discount-eligibility', user?.userId],
    queryFn: DiscountService.getEligibility,
    enabled: !!user?.userId && !!user?.birthDate,
  });

  useEffect(() => {
    if (!user?.userId || inFlight.current) {
      return;
    }
    if (user.birthDate) {
      return;
    }

    inFlight.current = true;
    Alert.alert('Complete your profile', 'Add your birth date to unlock youth or senior URBANOVA discounts.', [
      { text: 'Later', style: 'cancel' },
      { text: 'Go to Profile', onPress: onOpenProfile },
    ]);
    inFlight.current = false;
  }, [onOpenProfile, user?.birthDate, user?.userId]);

  useEffect(() => {
    const eligibility = eligibilityQuery.data;
    const userId = user?.userId;
    if (!userId || !eligibility || inFlight.current) {
      return;
    }

    const eligibleType = getUserFacingEligibilityType(eligibility);
    const estimatedPercentage = Number(eligibility.estimatedPercentage || 0);
    if (!eligibleType && estimatedPercentage <= 0) {
      return;
    }

    inFlight.current = true;
    const key = `${PROMOTION_PROMPT_PREFIX}${userId}:${eligibleType || 'discount'}:${estimatedPercentage}`;
    AsyncStorage.getItem(key)
      .then((shown) => {
        if (shown) {
          return;
        }
        return AsyncStorage.setItem(key, '1').then(() => {
          Alert.alert(
            'Promotion applied',
            `${getEligibilityHeadline(eligibility)} is active. Frequent rider eligibility is based on 8 hours of usage in the last 7 days. Eligible hire options and wallet prices will show the discount automatically.`
          );
        });
      })
      .finally(() => {
        inFlight.current = false;
      });
  }, [eligibilityQuery.data, user?.userId]);

  return null;
};

export default PromotionGate;
