package dev.mzkhawar.deenquestbackend.task_log;

import dev.mzkhawar.deenquestbackend.config.JwtService;
import dev.mzkhawar.deenquestbackend.task.Task;
import dev.mzkhawar.deenquestbackend.task.TaskRepository;
import dev.mzkhawar.deenquestbackend.user.Role;
import dev.mzkhawar.deenquestbackend.user.User;
import dev.mzkhawar.deenquestbackend.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TestTaskLogController {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TaskLogRepository taskLogRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;

    User testUser;
    Task testTask;
    Task testTask2;
    TaskLog testTaskLog;
    TaskLog testTaskLog2;
    TaskLog testTaskLog3;
    TaskLog testTaskLog4;
    String jwt;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .firstName("John")
                .lastName("Cena")
                .email("john_cena@wwe.com")
                .password(passwordEncoder.encode("asoDS3@21s"))
                .role(Role.USER)
                .build();
        userRepository.save(testUser);

        jwt = jwtService.generateJwt(testUser);

        testTask = Task.builder()
                .name("Test Task")
                .description("Test Description")
                .build();
        taskRepository.save(testTask);

        testTask2 = Task.builder()
                .name("Test Task 2")
                .description("Test Description 2")
                .build();
        taskRepository.save(testTask2);

        testTaskLog = TaskLog.builder()
                .task(testTask)
                .user(testUser)
                .completedAt(LocalDateTime.of(2025, 4, 30, 23, 59, 59)) // 2025-04-30 11:59:59 PM
                .build();
        taskLogRepository.save(testTaskLog);

        testTaskLog2 = TaskLog.builder()
                .task(testTask2)
                .user(testUser)
                .completedAt(LocalDateTime.of(2025, 5, 1, 0, 0)) // 2025-05-01 12:00 AM
                .build();
        taskLogRepository.save(testTaskLog2);

        testTaskLog3 = TaskLog.builder()
                .task(testTask)
                .user(testUser)
                .completedAt(LocalDateTime.of(2025, 5, 1, 23, 59, 59)) // 2025-05-01 11:59:59 PM
                .build();
        taskLogRepository.save(testTaskLog3);

        testTaskLog4 = TaskLog.builder()
                .task(testTask2)
                .user(testUser)
                .completedAt(LocalDateTime.of(2025, 5, 2, 0, 0)) // 2025-05-02 12:00 AM
                .build();
        taskLogRepository.save(testTaskLog4);
    }

    @AfterEach
    void tearDown() {
        taskLogRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void givenValidTaskLogRequest_whenCreateTaskLog_thenTaskLogIsPersistedAndLocationIsReturned() throws Exception {
        LocalDateTime completedAt = LocalDateTime.now();
        String taskLogRequestJson = """
                {
                    "taskId": "%d",
                    "completedAt": "%s"
                }
                """.formatted(testTask.getId(), completedAt);

        String createdTaskLogLocation = mockMvc.perform(post("/api/v1/task-logs")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskLogRequestJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse()
                .getHeader("Location");

        assertNotNull(createdTaskLogLocation);
        Long createdTaskLogId = Long.valueOf(createdTaskLogLocation.substring(
                createdTaskLogLocation.lastIndexOf('/') + 1));

        List<TaskLog> taskLogs = taskLogRepository.findAll();
        assertEquals(5, taskLogs.size());

        TaskLog createdTaskLog = taskLogs.stream().filter(taskLog ->
                taskLog.getId().equals(createdTaskLogId)).findFirst().orElseThrow();

        assertEquals(testTask.getId(), createdTaskLog.getTask().getId());
        assertEquals(testUser.getId(), createdTaskLog.getUser().getId());
        assertEquals(completedAt, createdTaskLog.getCompletedAt());

        assertNotNull(createdTaskLogLocation);
        assertTrue(createdTaskLogLocation.endsWith("api/v1/task-logs/" + createdTaskLogId));
    }

    @Test
    void givenExistingTaskLogs_whenGetAllTaskLogs_thenTaskLogsAreReturned() throws Exception {
        // todo
    }

    @Test
    void givenExistingTaskLog_whenGetTaskLogById_thenReturnsTaskLogWithAssociatedTask() throws Exception {
        mockMvc.perform(get("/api/v1/task-logs/" + testTaskLog.getId())
                .header("Authorization", "Bearer " + jwt))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.completedAt").value(testTaskLog.getCompletedAt().toString()),
                        jsonPath("$.task.id").value(testTask.getId()),
                        jsonPath("$.task.name").value(testTask.getName()),
                        jsonPath("$.task.description").value(testTask.getDescription())
                );
    }

    @Test
    void givenValidUpdateTaskLogRequest_whenUpdateTaskLog_thenUpdatesTaskLogAndReturnsNoContent() throws Exception {
        LocalDateTime updatedCompletedAt = LocalDateTime.now().minusDays(1);
        String updateTaskLogJson = """
                {
                    "taskId": "%d",
                    "completedAt": "%s"
                }
                """.formatted(testTask.getId(), updatedCompletedAt);

        mockMvc.perform(put("/api/v1/task-logs/" + testTaskLog.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateTaskLogJson))
                .andExpect(status().isNoContent());

        TaskLog updatedTaskLog = taskLogRepository.findById(testTaskLog.getId()).orElseThrow();
        assertEquals(updatedCompletedAt, updatedTaskLog.getCompletedAt());
    }

    @Test
    void givenExistingTaskLogs_whenGetTaskLogsByDateRange_thenReturnsFilteredTaskLogs() throws Exception {
        // Find task logs in the month of May
        LocalDateTime fromDate = LocalDateTime.of(2025, 5, 1, 0, 0); // 2025-05-01 12:00 AM
        LocalDateTime toDate = LocalDateTime.of(2025, 5, 31, 23, 59, 59); // 2025-05-31 12:59:59 PM
        mockMvc.perform(get("/api/v1/task-logs/date-range")
                .header("Authorization", "Bearer " + jwt)
                .queryParam("from", fromDate.toString())
                .queryParam("to", toDate.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(3),
                        jsonPath("$[0].completedAt").value(testTaskLog2.getCompletedAt()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))), // formatting to add seconds
                        jsonPath("$[1].completedAt").value(testTaskLog3.getCompletedAt().toString()),
                        jsonPath("$[2].completedAt").value(testTaskLog4.getCompletedAt()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))) // formatting to add seconds
                );
    }

    @Test
    void givenExistingTaskLogs_whenGetTaskLogsByDateRangeAndTask_thenReturnsFilteredTaskLogs() throws Exception {
        // Find task logs in the month of May with task: testTask2
        LocalDateTime fromDate = LocalDateTime.of(2025, 5, 1, 0, 0); // 2025-05-01 12:00 AM
        LocalDateTime toDate = LocalDateTime.of(2025, 5, 31, 23, 59, 59); // 2025-05-31 12:59:59 PM
        mockMvc.perform(get("/api/v1/task-logs/date-range/" + testTask2.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .queryParam("from", fromDate.toString())
                        .queryParam("to", toDate.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$[0].completedAt").value(testTaskLog2.getCompletedAt()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))), // formatting to add seconds
                        jsonPath("$[1].completedAt").value(testTaskLog4.getCompletedAt()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))) // formatting to add seconds
                );
    }

    @Test
    void givenExistingTaskLog_whenDeleteTaskLog_thenDeletesTaskLogAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/task-logs/" + testTaskLog.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        assertTrue(taskLogRepository.findById(testTaskLog.getId()).isEmpty());
    }
}