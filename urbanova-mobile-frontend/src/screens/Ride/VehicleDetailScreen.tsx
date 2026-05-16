import React, { useMemo, useState } from 'react';
import { ActivityIndicator, Alert, Image, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useMutation, useQuery } from '@tanstack/react-query';

import ScooterFaultDiagram, { FaultPart } from '@components/ScooterFaultDiagram';
import PrimaryButton from '@components/PrimaryButton';
import { getVehicleModelImage } from '@data/vehicleImages';
import { usePasses } from '@hooks/usePasses';
import { RootStackParamList } from '@models/index';
import { BookingService, IssueService, ScooterService } from '@services/api';
import { useRideStore } from '@store/useRideStore';
import { colors, radii } from '@theme/index';
import { formatCurrency } from '@utils/format';

type Props = NativeStackScreenProps<RootStackParamList, 'VehicleDetail'>;

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const PICKER_YEARS = Array.from({ length: 6 }, (_, index) => new Date().getFullYear() + index);

const getDayCount = (year: number, monthIndex: number) => new Date(year, monthIndex + 1, 0).getDate();

const roundUpToNextFiveMinutes = (date: Date) => {
  const rounded = new Date(date);
  rounded.setSeconds(0, 0);
  const remainder = rounded.getMinutes() % 5;
  if (remainder !== 0) {
    rounded.setMinutes(rounded.getMinutes() + (5 - remainder));
  }
  return rounded;
};

const formatPlannedStartDisplay = (value: string) => {
  if (!value) return 'Immediate start';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Immediate start';
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const VehicleDetailScreen: React.FC<Props> = ({ route, navigation }) => {
  const { vehicleId, mode = 'reserve' } = route.params;
  const plannedStartAt = useRideStore((state) => state.plannedStartAt);
  const setPlannedStartAt = useRideStore((state) => state.setPlannedStartAt);
  const { passes, isLoading: passesLoading } = usePasses();
  const [selectedHireOption, setSelectedHireOption] = useState<string | null>(null);
  const [reserving, setReserving] = useState(false);
  const [startPickerVisible, setStartPickerVisible] = useState(false);
  const [faultPart, setFaultPart] = useState<FaultPart>('brake');
  const [title, setTitle] = useState('Vehicle fault report');
  const [description, setDescription] = useState('');
  const [photos, setPhotos] = useState<Array<{ uri: string; name?: string; type?: string }>>([]);
  const [pickerYear, setPickerYear] = useState(new Date().getFullYear());
  const [pickerMonth, setPickerMonth] = useState(new Date().getMonth());
  const [pickerDay, setPickerDay] = useState(new Date().getDate());
  const [pickerHour, setPickerHour] = useState(new Date().getHours());
  const [pickerMinute, setPickerMinute] = useState(Math.round(new Date().getMinutes() / 5) * 5 % 60);

  const scooterQuery = useQuery({
    queryKey: ['scooter-detail', vehicleId],
    queryFn: () => ScooterService.getDetail(vehicleId),
  });

  const imageSource = useMemo(() => {
    const localImage = getVehicleModelImage(String(scooterQuery.data?.typeCode || ''));
    if (localImage) return localImage;
    if (scooterQuery.data?.typeImageUrl) return { uri: scooterQuery.data.typeImageUrl };
    return null;
  }, [scooterQuery.data?.typeCode, scooterQuery.data?.typeImageUrl]);

  const reportMutation = useMutation({
    mutationFn: async () => {
      const issue = await IssueService.create({
        scooterId: vehicleId,
        issueType: 'FAULT_REPORT',
        title: title.trim(),
        description: `[${faultPart.toUpperCase()}] ${description.trim()}`,
      });
      if (photos.length > 0) {
        await IssueService.uploadPhotos(issue.issueId, photos);
      }
      return issue;
    },
    onSuccess: () => {
      setDescription('');
      setPhotos([]);
      Alert.alert('Fault reported', 'Thanks. URBANOVA will review this vehicle before it is used again.');
    },
    onError: (error: any) => {
      Alert.alert('Report failed', error?.response?.data?.error?.message || 'Unable to submit fault report.');
    },
  });

  const scooter = scooterQuery.data;
  const canReserveFromDetail = mode !== 'report' && String(scooter?.status || '').toUpperCase() === 'AVAILABLE';
  const dayOptions = Array.from({ length: getDayCount(pickerYear, pickerMonth) }, (_, index) => index + 1);
  const hourOptions = Array.from({ length: 24 }, (_, index) => index);
  const minuteOptions = Array.from({ length: 12 }, (_, index) => index * 5);

  React.useEffect(() => {
    if (!selectedHireOption && passes.length > 0) {
      setSelectedHireOption(passes[0].id);
    }
  }, [passes, selectedHireOption]);

  const openStartPicker = () => {
    const now = roundUpToNextFiveMinutes(new Date());
    if (plannedStartAt) {
      const parsed = new Date(plannedStartAt);
      if (!Number.isNaN(parsed.getTime()) && parsed.getTime() >= now.getTime()) {
        setPickerYear(parsed.getFullYear());
        setPickerMonth(parsed.getMonth());
        setPickerDay(parsed.getDate());
        setPickerHour(parsed.getHours());
        setPickerMinute(Math.round(parsed.getMinutes() / 5) * 5 % 60);
      } else {
        setPickerYear(now.getFullYear());
        setPickerMonth(now.getMonth());
        setPickerDay(now.getDate());
        setPickerHour(now.getHours());
        setPickerMinute(now.getMinutes());
      }
    } else {
      setPickerYear(now.getFullYear());
      setPickerMonth(now.getMonth());
      setPickerDay(now.getDate());
      setPickerHour(now.getHours());
      setPickerMinute(now.getMinutes());
    }
    setStartPickerVisible(true);
  };

  const confirmStartPicker = () => {
    const selected = new Date(pickerYear, pickerMonth, pickerDay, pickerHour, pickerMinute, 0);
    if (selected.getTime() < Date.now() - 60_000) {
      Alert.alert('Invalid start time', 'Please choose the current time or a future time.');
      return;
    }
    setPlannedStartAt(selected.toISOString().slice(0, 16));
    setStartPickerVisible(false);
  };

  const handleReserve = async () => {
    if (!selectedHireOption) {
      Alert.alert('Select hire option', 'Please pick a hire option before reserving.');
      return;
    }
    if (plannedStartAt) {
      const plannedDate = new Date(plannedStartAt);
      if (Number.isNaN(plannedDate.getTime()) || plannedDate.getTime() < Date.now() - 60_000) {
        Alert.alert('Invalid start time', 'Please choose the current time or a future time.');
        return;
      }
    }

    try {
      setReserving(true);
      const booking = await BookingService.create({
        scooterId: vehicleId,
        hireOptionId: selectedHireOption,
        plannedStartAt: plannedStartAt.trim() || undefined,
      });
      navigation.navigate('RideDetail', { bookingId: booking.bookingId });
    } catch (error: any) {
      Alert.alert('Reservation failed', error?.response?.data?.error?.message || 'Unable to reserve this vehicle.');
    } finally {
      setReserving(false);
    }
  };

  const handlePickPhotos = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Photo permission needed', 'Allow photo access to attach fault images.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      selectionLimit: 5,
      quality: 0.82,
    });
    if (result.canceled) return;
    setPhotos(
      result.assets.slice(0, 5).map((asset: ImagePicker.ImagePickerAsset, index: number) => ({
        uri: asset.uri,
        name: asset.fileName || `vehicle-fault-${index + 1}.jpg`,
        type: asset.mimeType || 'image/jpeg',
      }))
    );
  };

  const handleSubmit = () => {
    if (!title.trim()) {
      Alert.alert('Missing title', 'Please add a short fault title.');
      return;
    }
    if (!description.trim()) {
      Alert.alert('Missing details', 'Please describe what is wrong with this vehicle.');
      return;
    }
    reportMutation.mutate();
  };

  if (scooterQuery.isLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator color={colors.lime} />
        <Text style={styles.helper}>Loading vehicle details...</Text>
      </View>
    );
  }

  return (
    <>
      <ScrollView style={styles.container} contentContainerStyle={styles.content}>
        <View style={styles.heroCard}>
          {imageSource ? <Image source={imageSource as any} style={styles.vehicleImage} resizeMode="cover" /> : null}
          <Text style={styles.kicker}>URBANOVA vehicle</Text>
          <Text style={styles.title}>{vehicleId}</Text>
          <Text style={styles.meta}>Model: {String(scooter?.typeDisplayName || scooter?.typeCode || 'Standard scooter')}</Text>
          <Text style={styles.meta}>Status: {String(scooter?.status || 'Unknown')}</Text>
          <Text style={styles.meta}>Battery: {scooter?.batteryPercent ?? '-'}%</Text>
          <Text style={styles.meta}>Zone: {String(scooter?.zone || 'Not assigned')}</Text>
        </View>

        {canReserveFromDetail ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Planned start time</Text>
          <Text style={styles.helper}>Optional. Leave empty to reserve for immediate use.</Text>
          <Pressable style={styles.dateButton} onPress={openStartPicker}>
            <Text style={styles.dateButtonText}>{formatPlannedStartDisplay(plannedStartAt)}</Text>
          </Pressable>

          <Text style={[styles.sectionTitle, styles.hireTitle]}>Hire option</Text>
          {passesLoading ? (
            <ActivityIndicator color={colors.lime} style={styles.inlineLoader} />
          ) : (
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.passRow}>
              {passes.map((pass) => (
                <Pressable
                  key={pass.id}
                  style={[styles.passChip, selectedHireOption === pass.id && styles.passChipActive]}
                  onPress={() => setSelectedHireOption(pass.id)}
                >
                  <Text style={styles.passName}>{pass.name}</Text>
                  <Text style={styles.passPrice}>{formatCurrency(pass.price)}</Text>
                  <Text style={styles.passHint}>{Math.round(pass.durationMinutes / 60)} hours</Text>
                </Pressable>
              ))}
            </ScrollView>
          )}
          <PrimaryButton
            label={reserving ? 'Reserving...' : 'Reserve this vehicle'}
            onPress={handleReserve}
            disabled={reserving || passesLoading || !selectedHireOption}
            style={styles.reserveButton}
          />
        </View>
        ) : null}

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Report a fault</Text>
          <Text style={styles.helper}>Tap the faulty part, add details, and optionally attach photos.</Text>
          <ScooterFaultDiagram selectedPart={faultPart} onSelectPart={setFaultPart} />
          <TextInput
            style={styles.input}
            placeholder="Fault title"
            placeholderTextColor={colors.textMuted}
            value={title}
            onChangeText={setTitle}
          />
          <TextInput
            style={[styles.input, styles.textarea]}
            placeholder="Describe the fault"
            placeholderTextColor={colors.textMuted}
            value={description}
            onChangeText={setDescription}
            multiline
          />
          <View style={styles.photoHeader}>
            <Text style={styles.photoLabel}>Photos ({photos.length}/5)</Text>
            <Pressable onPress={handlePickPhotos}>
              <Text style={styles.linkText}>Choose photos</Text>
            </Pressable>
          </View>
          {photos.length > 0 ? (
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.photoStrip}>
              {photos.map((photo) => (
                <View key={photo.uri} style={styles.photoThumbWrap}>
                  <Image source={{ uri: photo.uri }} style={styles.photoThumb} />
                  <Pressable style={styles.removePhoto} onPress={() => setPhotos((current) => current.filter((item) => item.uri !== photo.uri))}>
                    <Text style={styles.removePhotoText}>x</Text>
                  </Pressable>
                </View>
              ))}
            </ScrollView>
          ) : (
            <Text style={styles.helper}>No photos selected.</Text>
          )}
          <PrimaryButton
            label={reportMutation.isPending ? 'Submitting...' : 'Submit fault report'}
            disabled={reportMutation.isPending}
            onPress={handleSubmit}
          />
        </View>
      </ScrollView>

      <Modal visible={startPickerVisible} transparent animationType="fade" onRequestClose={() => setStartPickerVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.pickerCard}>
            <Text style={styles.sectionTitle}>Select start time</Text>
            <View style={styles.pickerHeader}>
              <Text style={styles.helper}>Year</Text>
              <Text style={styles.helper}>Month</Text>
              <Text style={styles.helper}>Day</Text>
              <Text style={styles.helper}>Hour</Text>
              <Text style={styles.helper}>Min</Text>
            </View>
            <View style={styles.pickerRow}>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {PICKER_YEARS.map((year) => (
                  <PickerItem key={year} label={`${year}`} selected={pickerYear === year} onPress={() => setPickerYear(year)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {MONTHS.map((month, index) => (
                  <PickerItem key={month} label={month} selected={pickerMonth === index} onPress={() => setPickerMonth(index)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {dayOptions.map((day) => (
                  <PickerItem key={day} label={`${day}`} selected={pickerDay === day} onPress={() => setPickerDay(day)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {hourOptions.map((hour) => (
                  <PickerItem key={hour} label={`${String(hour).padStart(2, '0')}`} selected={pickerHour === hour} onPress={() => setPickerHour(hour)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {minuteOptions.map((minute) => (
                  <PickerItem
                    key={minute}
                    label={`${String(minute).padStart(2, '0')}`}
                    selected={pickerMinute === minute}
                    onPress={() => setPickerMinute(minute)}
                  />
                ))}
              </ScrollView>
            </View>
            <View style={styles.pickerActions}>
              <PrimaryButton label="Close" onPress={() => setStartPickerVisible(false)} style={styles.pickerButton} />
              <PrimaryButton label="Confirm" onPress={confirmStartPicker} style={styles.pickerButton} />
            </View>
          </View>
        </View>
      </Modal>
    </>
  );
};

const PickerItem = ({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) => (
  <Pressable style={[styles.pickerItem, selected && styles.pickerItemSelected]} onPress={onPress}>
    <Text style={[styles.pickerItemLabel, selected && styles.pickerItemLabelSelected]}>{label}</Text>
  </Pressable>
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.ink,
  },
  content: {
    padding: 20,
    paddingTop: 96,
    paddingBottom: 40,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.ink,
  },
  heroCard: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 18,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  vehicleImage: {
    width: '100%',
    height: 240,
    borderRadius: radii.lg,
    marginBottom: 14,
    backgroundColor: 'rgba(255,255,255,0.06)',
  },
  kicker: {
    color: colors.lime,
    fontWeight: '800',
    textTransform: 'uppercase',
    fontSize: 12,
  },
  title: {
    color: colors.textPrimary,
    fontSize: 28,
    fontWeight: '800',
    marginTop: 6,
  },
  meta: {
    color: colors.textSecondary,
    marginTop: 6,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    marginBottom: 16,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '800',
  },
  helper: {
    color: colors.textSecondary,
    marginTop: 8,
    marginBottom: 10,
  },
  dateButton: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 12,
    marginTop: 8,
  },
  dateButtonText: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  hireTitle: {
    marginTop: 18,
  },
  inlineLoader: {
    marginTop: 14,
    alignSelf: 'flex-start',
  },
  passRow: {
    paddingTop: 12,
    paddingBottom: 4,
  },
  passChip: {
    minWidth: 120,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    backgroundColor: 'rgba(255,255,255,0.05)',
    padding: 12,
    marginRight: 10,
  },
  passChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.18)',
  },
  passName: {
    color: colors.textPrimary,
    fontWeight: '800',
  },
  passPrice: {
    color: colors.lime,
    fontSize: 16,
    fontWeight: '800',
    marginTop: 6,
  },
  passHint: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 3,
  },
  reserveButton: {
    marginTop: 14,
  },
  input: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: colors.textPrimary,
    marginBottom: 10,
  },
  textarea: {
    minHeight: 96,
    textAlignVertical: 'top',
  },
  photoHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  photoLabel: {
    color: colors.textSecondary,
    fontWeight: '700',
  },
  linkText: {
    color: colors.lime,
    fontWeight: '800',
  },
  photoStrip: {
    marginBottom: 12,
  },
  photoThumbWrap: {
    width: 74,
    height: 74,
    marginRight: 10,
  },
  photoThumb: {
    width: 74,
    height: 74,
    borderRadius: radii.md,
    backgroundColor: 'rgba(255,255,255,0.08)',
  },
  removePhoto: {
    position: 'absolute',
    top: -6,
    right: -6,
    width: 22,
    height: 22,
    borderRadius: 11,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.danger,
  },
  removePhotoText: {
    color: '#FFFFFF',
    fontWeight: '800',
    lineHeight: 18,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  pickerCard: {
    backgroundColor: colors.graphite,
    borderRadius: radii.lg,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  pickerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10,
  },
  pickerRow: {
    flexDirection: 'row',
    marginTop: 8,
    height: 220,
  },
  pickerColumn: {
    flex: 1,
    marginRight: 8,
  },
  pickerItem: {
    height: 40,
    borderRadius: radii.sm,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  pickerItemSelected: {
    backgroundColor: 'rgba(131,111,255,0.22)',
    borderWidth: 1,
    borderColor: colors.lime,
  },
  pickerItemLabel: {
    color: colors.textSecondary,
    fontWeight: '600',
  },
  pickerItemLabelSelected: {
    color: colors.textPrimary,
  },
  pickerActions: {
    flexDirection: 'row',
    marginTop: 12,
  },
  pickerButton: {
    flex: 1,
    marginRight: 10,
  },
});

export default VehicleDetailScreen;
