package com.example.hackathoncodaro2026.config;

import com.example.hackathoncodaro2026.model.Address;
import com.example.hackathoncodaro2026.model.Facility;
import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.PaymentMethod;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.FacilityRepository;
import com.example.hackathoncodaro2026.repository.InventoryItemRepository;
import com.example.hackathoncodaro2026.repository.ReservationRepository;
import com.example.hackathoncodaro2026.repository.SportResourceRepository;
import com.example.hackathoncodaro2026.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DataInitializer implements CommandLineRunner {

    private static final LocalTime OPEN = LocalTime.of(7, 0);
    private static final LocalTime CLOSE = LocalTime.of(22, 0);

    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final SportResourceRepository sportResourceRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            FacilityRepository facilityRepository,
            SportResourceRepository sportResourceRepository,
            ReservationRepository reservationRepository,
            InventoryItemRepository inventoryItemRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
        this.sportResourceRepository = sportResourceRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@sportsfacility.local");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setFullName("Facility Administrator");
            admin.setRole(Role.ADMIN);
            admin.setPhone("+48 22 621 00 01");
            admin.setEnabled(true);
            userRepository.save(admin);
        } else {
            userRepository.findByUsernameIgnoreCase("admin").ifPresent(admin -> {
                if (admin.getPhone() == null || admin.getPhone().isBlank()) {
                    admin.setPhone("+48 22 621 00 01");
                    userRepository.save(admin);
                }
            });
        }
        if (!userRepository.existsByUsernameIgnoreCase("manager")) {
            User manager = new User();
            manager.setUsername("manager");
            manager.setEmail("manager@sportsfacility.local");
            manager.setPassword(passwordEncoder.encode("Manager123!"));
            manager.setFullName("Court Manager");
            manager.setRole(Role.MANAGER);
            manager.setPhone("+48 22 621 00 02");
            manager.setEnabled(true);
            userRepository.save(manager);
        }
        if (facilityRepository.count() == 0) {
            seedWarsawNetwork();
        }
        backfillImagePaths();
        backfillPartySizes();
        backfillSwimCapacities();
        backfillLessonPartySizes();
        backfillHourlyPrices();
        backfillLessonPrices();
        seedInventory();
        backfillReservationFields();
    }

    private void seedWarsawNetwork() {
        Facility torwar = saveFacility(
                "COS Torwar",
                "Flagship indoor arena next to the National Stadium. Tennis, basketball, and a high-capacity gym.",
                "+48 22 621 44 11",
                address("ul. \u0141azienkowska", "6A", "00-449", "\u015Ar\u00F3dmie\u015Bcie"),
                ResourceType.TENNIS
        );
        saveResources(List.of(
                resource(torwar, "Tennis Court 1", ResourceType.TENNIS, address("ul. \u0141azienkowska", "6A", "00-449", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(torwar, "Tennis Court 2", ResourceType.TENNIS, address("ul. \u0141azienkowska", "6B", "00-449", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(torwar, "Main Basketball Hall", ResourceType.BASKETBALL, address("ul. \u0141azienkowska", "8", "00-449", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(torwar, "Performance Gym", ResourceType.GYM, address("ul. \u0141azienkowska", "6A", "00-449", "\u015Ar\u00F3dmie\u015Bcie"), 18)
        ));

        Facility inflancka = saveFacility(
                "Centrum Sportu Inflancka",
                "City sports centre in Muran\u00F3w with a full-size hall, volleyball court, and weights room.",
                "+48 22 831 20 91",
                address("ul. Inflancka", "8", "00-189", "\u015Ar\u00F3dmie\u015Bcie"),
                ResourceType.BASKETBALL
        );
        saveResources(List.of(
                resource(inflancka, "Main Hall", ResourceType.BASKETBALL, address("ul. Inflancka", "8", "00-189", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(inflancka, "Volleyball Court", ResourceType.VOLLEYBALL, address("ul. Inflancka", "10", "00-189", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(inflancka, "Studio Gym", ResourceType.GYM, address("ul. Inflancka", "8A", "00-189", "\u015Ar\u00F3dmie\u015Bcie"), 16)
        ));

        Facility awf = saveFacility(
                "AWF Warszawa",
                "University of Physical Education campus in Bielany. Pitches, pool lanes, and tennis.",
                "+48 22 834 04 31",
                address("ul. Marymoncka", "34", "01-813", "Bielany"),
                ResourceType.FOOTBALL
        );
        saveResources(List.of(
                resource(awf, "Football Pitch A", ResourceType.FOOTBALL, address("ul. Marymoncka", "34", "01-813", "Bielany"), 1),
                resource(awf, "Tennis Court North", ResourceType.TENNIS, address("ul. Marymoncka", "34A", "01-813", "Bielany"), 1),
                resource(awf, "Pool Lane 1", ResourceType.SWIMMING, address("ul. Marymoncka", "36", "01-813", "Bielany"), 8),
                resource(awf, "Training Gym", ResourceType.GYM, address("ul. Marymoncka", "32", "01-813", "Bielany"), 20)
        ));

        Facility agrykola = saveFacility(
                "O\u015Brodek Agrykola",
                "Historic club grounds below the Ujazdowski escarpment. Clay tennis and a riverside pitch.",
                "+48 22 621 47 41",
                address("ul. My\u015Bliwiecka", "9", "00-459", "\u015Ar\u00F3dmie\u015Bcie"),
                ResourceType.TENNIS
        );
        saveResources(List.of(
                resource(agrykola, "Clay Tennis Court", ResourceType.TENNIS, address("ul. My\u015Bliwiecka", "9", "00-459", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(agrykola, "Football Pitch", ResourceType.FOOTBALL, address("ul. My\u015Bliwiecka", "11", "00-459", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(agrykola, "Outdoor Basketball", ResourceType.BASKETBALL, address("ul. My\u015Bliwiecka", "9A", "00-459", "\u015Ar\u00F3dmie\u015Bcie"), 1)
        ));

        Facility warszawianka = saveFacility(
                "KS Warszawianka",
                "Multi-sport club in \u015Ar\u00F3dmie\u015Bcie with tennis, squash, and a 25 m pool.",
                "+48 22 628 80 71",
                address("ul. Szwole\u017Cer\u00F3w", "9", "00-464", "\u015Ar\u00F3dmie\u015Bcie"),
                ResourceType.TENNIS
        );
        saveResources(List.of(
                resource(warszawianka, "Tennis Court 1", ResourceType.TENNIS, address("ul. Szwole\u017Cer\u00F3w", "9", "00-464", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(warszawianka, "Tennis Court 2", ResourceType.TENNIS, address("ul. Szwole\u017Cer\u00F3w", "11", "00-464", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(warszawianka, "Squash Court A", ResourceType.SQUASH, address("ul. Szwole\u017Cer\u00F3w", "7", "00-464", "\u015Ar\u00F3dmie\u015Bcie"), 1),
                resource(warszawianka, "Pool Lane 2", ResourceType.SWIMMING, address("ul. Szwole\u017Cer\u00F3w", "9A", "00-464", "\u015Ar\u00F3dmie\u015Bcie"), 6)
        ));

        Facility kolo = saveFacility(
                "Hala Sportowa Ko\u0142o",
                "Indoor halls on the Wola side of the tracks. Basketball, volleyball, and a community gym.",
                "+48 22 632 11 80",
                address("ul. Obozowa", "63", "01-425", "Wola"),
                ResourceType.BASKETBALL
        );
        saveResources(List.of(
                resource(kolo, "Basketball Hall", ResourceType.BASKETBALL, address("ul. Obozowa", "63", "01-425", "Wola"), 1),
                resource(kolo, "Volleyball Hall", ResourceType.VOLLEYBALL, address("ul. Obozowa", "61", "01-425", "Wola"), 1),
                resource(kolo, "Community Gym", ResourceType.GYM, address("ul. Obozowa", "65", "01-425", "Wola"), 14)
        ));

        Facility orlik = saveFacility(
                "Orlik Wo\u0142oska",
                "Neighbourhood Orlik pitches next to the hospital campus in Mokot\u00F3w.",
                "+48 22 566 91 00",
                address("ul. Wo\u0142oska", "4", "02-561", "Mokot\u00F3w"),
                ResourceType.FOOTBALL
        );
        saveResources(List.of(
                resource(orlik, "Football Pitch 1", ResourceType.FOOTBALL, address("ul. Wo\u0142oska", "4", "02-561", "Mokot\u00F3w"), 1),
                resource(orlik, "Football Pitch 2", ResourceType.FOOTBALL, address("ul. Wo\u0142oska", "6", "02-561", "Mokot\u00F3w"), 1),
                resource(orlik, "Basketball Cage", ResourceType.BASKETBALL, address("ul. Wo\u0142oska", "4A", "02-561", "Mokot\u00F3w"), 1)
        ));

        Facility szczescie = saveFacility(
                "O\u015Brodek Szcz\u0119\u015Bliwice",
                "Park Szcz\u0119\u015Bliwicki sports cluster in Ochota. Tennis, football, and a hillside gym.",
                "+48 22 822 30 21",
                address("ul. Drawska", "22", "02-202", "Ochota"),
                ResourceType.TENNIS
        );
        saveResources(List.of(
                resource(szczescie, "Park Tennis Court", ResourceType.TENNIS, address("ul. Drawska", "22", "02-202", "Ochota"), 1),
                resource(szczescie, "Park Football Pitch", ResourceType.FOOTBALL, address("ul. Drawska", "20", "02-202", "Ochota"), 1),
                resource(szczescie, "Hillside Gym", ResourceType.GYM, address("ul. Drawska", "24", "02-202", "Ochota"), 12)
        ));

        Facility zoliborz = saveFacility(
                "O\u015Brodek \u017Boliborz",
                "District centre on Potocka with indoor volleyball and a compact gym.",
                "+48 22 839 44 50",
                address("ul. Potocka", "1", "01-634", "\u017Boliborz"),
                ResourceType.VOLLEYBALL
        );
        saveResources(List.of(
                resource(zoliborz, "Volleyball Hall", ResourceType.VOLLEYBALL, address("ul. Potocka", "1", "01-634", "\u017Boliborz"), 1),
                resource(zoliborz, "Tennis Court", ResourceType.TENNIS, address("ul. Potocka", "3", "01-634", "\u017Boliborz"), 1),
                resource(zoliborz, "District Gym", ResourceType.GYM, address("ul. Potocka", "1A", "01-634", "\u017Boliborz"), 15)
        ));

        Facility praga = saveFacility(
                "Hala Sportowa Praga",
                "East-bank hall on Kaw\u0119czy\u0144ska. Basketball, squash, and a busy gym floor.",
                "+48 22 818 08 92",
                address("ul. Kaw\u0119czy\u0144ska", "36", "03-772", "Praga-P\u00F3\u0142noc"),
                ResourceType.BASKETBALL
        );
        saveResources(List.of(
                resource(praga, "Basketball Hall", ResourceType.BASKETBALL, address("ul. Kaw\u0119czy\u0144ska", "36", "03-772", "Praga-P\u00F3\u0142noc"), 1),
                resource(praga, "Squash Court B", ResourceType.SQUASH, address("ul. Kaw\u0119czy\u0144ska", "38", "03-772", "Praga-P\u00F3\u0142noc"), 1),
                resource(praga, "East Gym", ResourceType.GYM, address("ul. Kaw\u0119czy\u0144ska", "34", "03-772", "Praga-P\u00F3\u0142noc"), 20)
        ));
    }

    private Address address(String street, String buildingNumber, String postalCode, String district) {
        return new Address(street, buildingNumber, postalCode, district);
    }

    private Facility saveFacility(String name, String description, String phone, Address address, ResourceType coverType) {
        Facility facility = new Facility();
        facility.setName(name);
        facility.setDescription(description);
        facility.setPhone(phone);
        facility.setAddress(address);
        facility.setEnabled(true);
        facility.setImagePath(coverType.getImagePath());
        return facilityRepository.save(facility);
    }

    private SportResource resource(Facility facility, String name, ResourceType type, Address address, int capacity) {
        SportResource resource = new SportResource();
        resource.setFacility(facility);
        resource.setName(name);
        resource.setType(type);
        resource.setAddress(address);
        resource.setCapacity(capacity);
        resource.setSlotDurationMinutes(60);
        resource.setOpeningTime(OPEN);
        resource.setClosingTime(CLOSE);
        resource.setEnabled(true);
        resource.setImagePath(type.getImagePath());
        resource.setMinPartySize(type.getMinPartySize());
        resource.setMaxPartySize(type.getMaxPartySize());
        applyLessonPartyRange(resource);
        resource.setBaseHourlyPrice(type.getBaseHourlyPrice());
        resource.setLessonHourlyPrice(type.getLessonHourlyPrice());
        return resource;
    }

    private void saveResources(List<SportResource> resources) {
        sportResourceRepository.saveAll(resources);
    }

    private void backfillImagePaths() {
        for (SportResource resource : sportResourceRepository.findAll()) {
            if (resource.getImagePath() == null || resource.getImagePath().isBlank()) {
                resource.setImagePath(resource.getType().getImagePath());
                sportResourceRepository.save(resource);
            }
        }
        for (Facility facility : facilityRepository.findAll()) {
            if (facility.getImagePath() == null || facility.getImagePath().isBlank()) {
                List<SportResource> resources = sportResourceRepository.findByFacility_IdAndEnabledTrueOrderByNameAsc(facility.getId());
                if (!resources.isEmpty()) {
                    facility.setImagePath(resources.getFirst().getType().getImagePath());
                } else {
                    facility.setImagePath(ResourceType.TENNIS.getImagePath());
                }
                facilityRepository.save(facility);
            }
        }
    }

    private void backfillPartySizes() {
        for (SportResource resource : sportResourceRepository.findAll()) {
            if (needsPartySizeBackfill(resource)) {
                resource.setMinPartySize(resource.getType().getMinPartySize());
                resource.setMaxPartySize(resource.getType().getMaxPartySize());
                sportResourceRepository.save(resource);
            }
        }
    }

    private boolean needsPartySizeBackfill(SportResource resource) {
        int min = resource.getMinPartySize();
        int max = resource.getMaxPartySize();
        if (min < 1 || max < 1 || max < min) {
            return true;
        }
        return min == 1 && max == 1 && resource.getType().getMaxPartySize() > 1;
    }

    private void backfillSwimCapacities() {
        for (SportResource resource : sportResourceRepository.findAll()) {
            if (resource.getType() != ResourceType.SWIMMING || resource.getCapacity() > 1) {
                continue;
            }
            String name = resource.getName() == null ? "" : resource.getName();
            resource.setCapacity(name.contains("2") ? 6 : 8);
            sportResourceRepository.save(resource);
        }
    }

    private void backfillLessonPartySizes() {
        for (SportResource resource : sportResourceRepository.findAll()) {
            int min = resource.getLessonMinPartySize();
            int max = resource.getLessonMaxPartySize();
            applyLessonPartyRange(resource);
            if (resource.getLessonMinPartySize() != min || resource.getLessonMaxPartySize() != max) {
                sportResourceRepository.save(resource);
            }
        }
    }

    private void applyLessonPartyRange(SportResource resource) {
        if (resource.getMinPartySize() == 1 && resource.getMaxPartySize() == 1 && resource.getCapacity() > 1) {
            resource.setLessonMinPartySize(2);
            resource.setLessonMaxPartySize(resource.getCapacity());
            return;
        }
        resource.setLessonMinPartySize(1);
        resource.setLessonMaxPartySize(1);
    }

    private void backfillHourlyPrices() {
        for (SportResource resource : sportResourceRepository.findAll()) {
            if (resource.getBaseHourlyPrice() == null || resource.getBaseHourlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
                resource.setBaseHourlyPrice(resource.getType().getBaseHourlyPrice());
                sportResourceRepository.save(resource);
            }
        }
    }

    private void backfillLessonPrices() {
        for (SportResource resource : sportResourceRepository.findAll()) {
            if (resource.getLessonHourlyPrice() == null || resource.getLessonHourlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
                BigDecimal lesson = resource.getType().getLessonHourlyPrice();
                if (lesson != null && lesson.compareTo(BigDecimal.ZERO) > 0) {
                    resource.setLessonHourlyPrice(lesson);
                    sportResourceRepository.save(resource);
                }
            }
        }
    }

    private void seedInventory() {
        seedItem("Racket", "15.00", ResourceType.TENNIS);
        seedItem("Ball basket", "12.00", ResourceType.TENNIS);
        seedItem("Racket", "15.00", ResourceType.SQUASH);
        seedItem("Balls", "8.00", ResourceType.SQUASH);
        seedItem("Ball", "10.00", ResourceType.FOOTBALL);
        seedItem("Bibs", "12.00", ResourceType.FOOTBALL);
        seedItem("Ball", "10.00", ResourceType.BASKETBALL);
        seedItem("Ball", "10.00", ResourceType.VOLLEYBALL);
        seedItem("Towel", "8.00", ResourceType.GYM);
        seedItem("Locker", "6.00", ResourceType.GYM);
        seedItem("Towel", "8.00", ResourceType.SWIMMING);
        seedItem("Goggles", "10.00", ResourceType.SWIMMING);
    }

    private void seedItem(String name, String price, ResourceType type) {
        if (inventoryItemRepository.existsByNameIgnoreCaseAndResourceType(name, type)) {
            return;
        }
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setPricePerPerson(new BigDecimal(price));
        item.setResourceType(type);
        item.setEnabled(true);
        inventoryItemRepository.save(item);
    }

    private void backfillReservationFields() {
        for (Reservation reservation : reservationRepository.findAll()) {
            boolean changed = false;
            if (reservation.getPartySize() < 1) {
                reservation.setPartySize(1);
                changed = true;
            }
            if (reservation.getPaymentMethod() == null) {
                reservation.setPaymentMethod(PaymentMethod.CASH);
                changed = true;
            }
            if (reservation.getTotalAmount() == null) {
                reservation.setTotalAmount(BigDecimal.ZERO.setScale(2));
                changed = true;
            }
            if (reservation.getKind() == null) {
                reservation.setKind(ReservationKind.STANDARD);
                changed = true;
            }
            if (reservation.getOccupancyUnits() < 1) {
                reservation.setOccupancyUnits(1);
                changed = true;
            }
            if (changed) {
                reservationRepository.save(reservation);
            }
        }
    }
}
