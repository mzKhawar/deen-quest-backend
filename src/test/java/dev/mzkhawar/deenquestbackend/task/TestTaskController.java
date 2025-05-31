package dev.mzkhawar.deenquestbackend.task;

import dev.mzkhawar.deenquestbackend.config.JwtService;
import dev.mzkhawar.deenquestbackend.user.Role;
import dev.mzkhawar.deenquestbackend.user.User;
import dev.mzkhawar.deenquestbackend.user.UserRepository;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TestTaskController {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;

    User testUser;
    Task testTask;
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
    }

    @AfterEach
    void tearDown() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void givenValidTaskRequest_whenCreateTask_thenPersistsTaskAndReturnsTaskLocation() throws Exception {
        String taskName = "New Task";
        String taskDescription = "New Test Description";
        String taskRequestJson = """
                {
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(taskName, taskDescription);

        String createdTaskLocation = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskRequestJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse()
                .getHeader("Location");

        List<Task> tasks = taskRepository.findAll();
        assertEquals(2, tasks.size());

        Task createdTask = tasks.stream().filter(task -> task.getName().equals(taskName)).findFirst().orElseThrow();

        assertEquals(taskName, createdTask.getName());
        assertEquals(taskDescription, createdTask.getDescription());

        Long createdTaskId = createdTask.getId();

        assertNotNull(createdTaskLocation);
        assertTrue(createdTaskLocation.endsWith("api/v1/tasks/" + createdTaskId));
    }

    @Test
    void givenMissingName_whenCreateTask_thenReturnsBadRequest() throws Exception {
        String taskRequestJson = """
                {
                    "description": "%s"
                }
                """;

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskRequestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenExistingTasks_whenGetAllTasks_thenReturnsAllTasks() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + jwt))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$[0].name").value(testTask.getName()),
                        jsonPath("$[0].description").value(testTask.getDescription())
                );
    }

    @Test
    void givenExistingTask_whenGetTaskById_thenReturnsTask() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/" + testTask.getId())
                .header("Authorization", "Bearer " + jwt))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.name").value(testTask.getName()),
                        jsonPath("$.description").value(testTask.getDescription())
                );
    }

    @Test
    void givenInvalidTaskId_whenGetTaskById_thenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/123")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenValidTaskRequest_whenUpdateTask_thenUpdatesTaskAndReturnsNoContent() throws Exception {
        String updatedTaskName = "Updated Task Name";
        String updatedTaskDescription = "Updated Description";
        String taskUpdateRequest = """
                {
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(updatedTaskName, updatedTaskDescription);

        mockMvc.perform(put("/api/v1/tasks/" + testTask.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskUpdateRequest))
                .andExpect(status().isNoContent());

        Task updatedTask = taskRepository.findById(testTask.getId()).orElseThrow();
        assertEquals(updatedTaskName, updatedTask.getName());
        assertEquals(updatedTaskDescription, updatedTask.getDescription());
    }

    @Test
    void givenValidTaskRequest_whenDeleteTask_thenDeletesTaskAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/" + testTask.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        assertTrue(taskRepository.findById(testTask.getId()).isEmpty());
    }
}
