package com.examlens.service;

import com.examlens.dao.AiJobDao;
import com.examlens.model.AiJob;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves whether persisted AI-generated records came from the simulated provider.
 * Saved rows only store an {@code ai_job_id}, so the provider is read back from {@code ai_jobs}
 * instead of being duplicated onto every table. Simulated results must always be labelled as such
 * in the UI; this keeps that labelling honest after a page reload.
 */
final class SimulatedFlags {

    private final AiJobDao jobs;

    SimulatedFlags(AiJobDao jobs) {
        this.jobs = jobs;
    }

    Map<Long, Boolean> forExam(long userId, long examId) {
        Map<Long, Boolean> map = new HashMap<>();
        try {
            for (AiJob job : jobs.recentForExam(examId, userId, 500)) {
                map.put(job.getId(), job.isSimulatedProvider());
            }
        } catch (SQLException e) {
            // A missing flag must never break a read; unknown is treated as "not simulated".
        }
        return map;
    }

    boolean forJob(long userId, Long jobId) {
        if (jobId == null) {
            return false;
        }
        try {
            return jobs.find(jobId, userId).map(AiJob::isSimulatedProvider).orElse(false);
        } catch (SQLException e) {
            return false;
        }
    }
}
