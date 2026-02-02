package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import com.hcmute.codesphere_server.model.entity.TestCaseEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.response.TestCaseResponse;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.repository.common.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    public TestCaseResponse createTestCase(CreateTestCaseRequest request) {
        // Kiểm tra problem tồn tại
        ProblemEntity problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

        // Tạo test case mới
        TestCaseEntity testCase = TestCaseEntity.builder()
                .problem(problem)
                .input(request.getInput())
                .expectedOutput(request.getExpectedOutput())
                .isSample(request.getIsSample() != null ? request.getIsSample() : false)
                .isHidden(request.getIsHidden() != null ? request.getIsHidden() : false)
                .weight(request.getWeight() != null ? request.getWeight() : 1)
                .isDeleted(false)
                .build();

        testCase = testCaseRepository.save(testCase);

        // Map sang response
        return TestCaseResponse.builder()
                .id(testCase.getId())
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .isSample(testCase.getIsSample())
                .isHidden(testCase.getIsHidden())
                .weight(testCase.getWeight())
                .build();
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long id, UpdateTestCaseRequest request) {
        // Kiểm tra testcase tồn tại
        TestCaseEntity testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testcase không tồn tại"));

        // Cập nhật các field nếu có
        if (request.getInput() != null) {
            testCase.setInput(request.getInput());
        }
        if (request.getExpectedOutput() != null) {
            testCase.setExpectedOutput(request.getExpectedOutput());
        }
        if (request.getIsSample() != null) {
            testCase.setIsSample(request.getIsSample());
        }
        if (request.getIsHidden() != null) {
            testCase.setIsHidden(request.getIsHidden());
        }
        if (request.getWeight() != null) {
            testCase.setWeight(request.getWeight());
        }

        testCase = testCaseRepository.save(testCase);

        // Map sang response
        return TestCaseResponse.builder()
                .id(testCase.getId())
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .isSample(testCase.getIsSample())
                .isHidden(testCase.getIsHidden())
                .weight(testCase.getWeight())
                .build();
    }

    @Transactional
    public void deleteTestCase(Long id) {
        // Kiểm tra testcase tồn tại
        TestCaseEntity testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testcase không tồn tại"));

        // Soft delete
        testCase.setIsDeleted(true);
        testCaseRepository.save(testCase);
    }
}

