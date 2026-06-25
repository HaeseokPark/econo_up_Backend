package com.econoup;

import com.econoup.dailyconnect.DailyArticleRepository;
import com.econoup.simulation.SimulationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class EconoupApplicationTests {
    @Autowired
    EntityManager entityManager;

    @Autowired
    SimulationRepository simulationRepository;

    @Autowired
    DailyArticleRepository dailyArticleRepository;

    @Test
    void createsSchemaAndSeedsOperationalContent() {
        assertThat(entityManager.getMetamodel().getEntities()).isNotEmpty();
        assertThat(simulationRepository.count()).isEqualTo(11);
        assertThat(dailyArticleRepository.count()).isPositive();
    }
}
