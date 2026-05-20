# CQRS Architecture Guide

## Quick Reference

### How to Use the Command Service

#### In the Controller
```java
// Create and dispatch a command
UpdateProviderFirmCommand command = 
    new UpdateProviderFirmCommand(providerId, patch);

ProviderCreationResult result = 
    providerFirmCommandService.handle(command);
```

#### In Functions/Methods
```java
// Commands can be created anywhere and dispatched through the service
public void processUpdate(String providerId, ProviderPatchV2 patch) {
    var command = new UpdateProviderFirmCommand(providerId, patch);
    var result = commandService.handle(command);
    // Use result as needed
}
```

### Adding a New Command Type

1. **Create the Command Record**
```java
public record MyNewCommand(String providerId, MyPayload payload) {
    public void validate() {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload required");
        }
    }
}
```

2. **Add Handler Method to Service Interface**
```java
public interface ProviderFirmCommandService {
    ProviderCreationResult handle(UpdateProviderFirmCommand command);
    MyResult handle(MyNewCommand command);  // Add this
}
```

3. **Implement Handler in DefaultService**
```java
@Override
public MyResult handle(MyNewCommand command) {
    command.validate();
    log.debug("Handling MyNewCommand for provider: {}", command.providerId());
    // Delegate to appropriate service
    return myService.doSomething(command.providerId(), command.payload());
}
```

4. **Add Tests**
```java
@Test
void handle_myNewCommand_returnsExpectedResult() {
    MyNewCommand command = new MyNewCommand(...);
    MyResult result = commandService.handle(command);
    assertThat(result).isEqualTo(expected);
}
```

### Command vs Query Pattern

| Aspect | Command | Query |
|--------|---------|-------|
| Purpose | Modifies state | Reads state |
| Side Effects | Yes | None |
| Return Value | Result of action | Data from read model |
| Caching | No | Yes |
| Scalable? | Requires coordination | Highly scalable |

### Current Implementation (Phase 2)

**In scope:**
- ✓ UpdateProviderFirmCommand
- ✓ Synchronous processing
- ✓ Exception handling via IllegalArgumentException
- ✓ Basic logging

**Not in scope (future phases):**
- Event publishing
- Async/async command processing
- Complex saga patterns
- Event sourcing
- Read model projections

## Testing Patterns

### Command Test
```java
@Test
void validate_validCommand_doesNotThrow() {
    UpdateProviderFirmCommand cmd = 
        new UpdateProviderFirmCommand(guid.toString(), patch);
    cmd.validate();  // Should not throw
}
```

### Handler Test
```java
@Test
void handle_validCommand_returnsExpectedResult() {
    when(providerService.patchProvider(id, patch))
        .thenReturn(expectedResult);
    
    ProviderCreationResult result = 
        commandService.handle(command);
    
    assertThat(result).isEqualTo(expectedResult);
}
```

### Controller Test
```java
@Test
void patchProviderFirm_returns200() throws Exception {
    when(commandService.handle(any(UpdateProviderFirmCommand.class)))
        .thenReturn(expectedResult);
    
    mockMvc.perform(patch("/provider-firms/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isOk());
}
```

## Dependency Flow

```
Controller
    ↓
ProviderFirmCommandService (interface)
    ↓
DefaultProviderFirmCommandService (implementation)
    ↓ delegates to
ProviderService
    ↓ uses
Repository
    ↓
Database
```

## Error Handling

Commands validate themselves:
```java
public void validate() {
    if (providerId == null || providerId.isBlank()) {
        throw new IllegalArgumentException("providerId must be provided");
    }
}
```

The service calls validate before processing:
```java
public ProviderCreationResult handle(UpdateProviderFirmCommand command) {
    command.validate();  // Throws if invalid
    // ... continue processing
}
```

## Logging

The service logs at debug level:
```java
log.debug("Handling UpdateProviderFirmCommand for provider: {}", 
    command.providerFirmId());
```

Enable debug logging for command service:
```
logging.level.uk.gov.justice.laa.providerdata.command=DEBUG
```

## Key Principles

1. **Commands are immutable** - Use records, not classes
2. **Commands validate themselves** - Not the handler
3. **One command = one action** - Single responsibility
4. **Handlers are thin** - Mostly delegation to services
5. **No command logic in controllers** - Controllers create and dispatch only

