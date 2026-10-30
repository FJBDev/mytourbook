# MCP Server Integration for MyTourBook

This documentation explains how to use the Model Context Protocol (MCP) server implementation for MyTourBook.

## Overview

The MCP (Model Context Protocol) Server implementation provides a way to integrate MyTourBook with AI models like Claude through a standardized protocol. This enables AI models to query tour data, generate statistics, and perform other operations on your tour database.

## Architecture

### Core Components

1. **MCPServerManager** - Main manager class for server lifecycle
   - Handles initialization, startup, and shutdown of the MCP server
   - Manages thread pools for server operations
   - Provides singleton pattern for single server instance
   - Thread-safe operations with synchronized access

2. **MCPServerIntegration** - Integration layer for tools and resources
   - Defines and manages MCP tools available to AI models
   - Manages resources that can be accessed by the server
   - Provides utility classes for tool and resource definitions

3. **MCPServerUsageExample** - Example implementations
   - Demonstrates various usage patterns
   - Shows how to initialize and use the server
   - Includes complete lifecycle management examples

## Dependencies

Add the following to your `MANIFEST.MF`:

```
Import-Package: io.modelcontextprotocol.sdk.server,
 ...other imports...
```

The MCP SDK library must be available in your classpath:
```xml
<dependency>
    <groupId>io.modelcontextprotocol</groupId>
    <artifactId>sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage Examples

### Example 1: Basic Server Setup

```java
MCPServerManager manager = MCPServerManager.getInstance();

// Initialize the server
if (!manager.initializeServer()) {
    System.err.println("Failed to initialize MCP server");
    return;
}

// Start the server
if (!manager.startServer()) {
    System.err.println("Failed to start MCP server");
    return;
}

System.out.println("MCP Server is running: " + manager.isServerRunning());

// When done:
// manager.stopServer();
```

### Example 2: Register Custom Tools

```java
MCPServerIntegration.MCPTool customTool = new MCPServerIntegration.MCPTool(
    "analyze_tour",
    "Analyze a specific tour and provide insights",
    "{\"type\":\"object\",\"properties\":{\"tour_id\":{\"type\":\"string\"}}}"
);

MCPServerIntegration.registerTool(customTool);
```

### Example 3: Register Custom Resources

```java
MCPServerIntegration.MCPResource customResource = new MCPServerIntegration.MCPResource(
    "mytourbook://gps-data",
    "application/json",
    "GPS data from imported tours"
);

MCPServerIntegration.registerResource(customResource);
```

### Example 4: List All Tools and Resources

```java
// Get all registered tools
MCPServerIntegration.getAllTools().forEach((name, tool) -> {
    System.out.println("Tool: " + tool.name + " - " + tool.description);
});

// Get all registered resources
MCPServerIntegration.getAllResources().forEach((uri, resource) -> {
    System.out.println("Resource: " + resource.uri + " - " + resource.description);
});
```

### Example 5: Server Restart

```java
MCPServerManager manager = MCPServerManager.getInstance();

if (manager.restartServer()) {
    System.out.println("Server restarted successfully");
} else {
    System.err.println("Server restart failed");
}
```

## Default Tools

The MCP server comes with the following default tools:

1. **query_tours** - Query tours from the MyTourBook database
   - Filters: Various tour query parameters
   - Usage: Get tours based on date range, type, distance, etc.

2. **get_tour_statistics** - Get statistics about tours
   - Parameters: tour_id
   - Returns: Speed, distance, duration, elevation gain/loss

3. **import_tour_data** - Import tour data from files
   - Parameters: file_path
   - Supported formats: FIT, GPX, TCX, etc.

4. **export_tour_data** - Export tour data to various formats
   - Parameters: tour_id, format
   - Formats: GPX, TCX, CSV, JSON, etc.

## Default Resources

The MCP server exposes the following resources:

1. **mytourbook://tours** - All tour data in JSON format
2. **mytourbook://statistics** - Tour statistics and summaries
3. **mytourbook://settings** - MyTourBook application settings

## Integration with RawDataManager

To integrate MCP server with the RawDataManager:

```java
public class RawDataManager {
    
    public void initializeMCPServer() {
        final MCPServerManager mcpManager = MCPServerManager.getInstance();
        
        if (mcpManager.initializeServer()) {
            // Register tour import/export tools
            MCPServerIntegration.registerTool(
                new MCPServerIntegration.MCPTool(
                    "import_tours_directly",
                    "Import tours directly from MCP client",
                    "{...}"
                )
            );
            
            mcpManager.startServer();
        }
    }
}
```

## Thread Safety

All MCP server operations are thread-safe:
- `MCPServerManager` uses synchronized blocks with a lock object
- Multiple threads can safely call server methods concurrently
- Server state changes are properly synchronized

## Server Lifecycle

1. **Initialization Phase**
   - Create server instance
   - Initialize thread pools
   - Load default tools and resources

2. **Running Phase**
   - Server accepts connections
   - Processes requests from AI models
   - Manages resources and tools

3. **Shutdown Phase**
   - Stop accepting new connections
   - Wait for in-flight requests to complete
   - Release thread pools
   - Clean up resources

## Error Handling

All server operations return boolean or throw exceptions. Always check return values:

```java
if (!manager.initializeServer()) {
    // Handle initialization error
}

if (!manager.startServer()) {
    // Handle startup error
}

if (!manager.stopServer()) {
    // Handle shutdown error
}
```

## Configuration

Server configuration can be modified in `MCPServerManager`:

```java
private static final String SERVER_NAME = "MyTourBook";
private static final String SERVER_VERSION = "1.0.0";
```

## Monitoring

Check server status at any time:

```java
MCPServerManager manager = MCPServerManager.getInstance();
boolean isRunning = manager.isServerRunning();
```

## Performance Considerations

1. **Thread Pool Size** - Currently uses 2 threads for server operations
2. **Request Handling** - Requests are processed asynchronously
3. **Resource Management** - Resources are loaded on-demand
4. **Memory Usage** - Scales with number of tours in database

## Security Notes

1. Ensure only trusted clients can connect to the MCP server
2. Validate all incoming requests for tour IDs and parameters
3. Implement access control for sensitive tour data
4. Consider encryption for data transmission

## Troubleshooting

### Server fails to initialize
- Check that `io.modelcontextprotocol.sdk.server` is in classpath
- Verify Java version compatibility (Java 21 required)
- Check system resources (ports, memory)

### Server crashes on startup
- Check logs for port conflicts
- Verify thread pool creation
- Check for null pointer exceptions

### Requests fail to process
- Ensure server is in running state
- Check request format matches schema
- Verify tour IDs exist in database

## API Reference

See the JavaDoc comments in the following classes:
- `MCPServerManager.java` - Main server lifecycle API
- `MCPServerIntegration.java` - Tool and resource definitions
- `MCPServerUsageExample.java` - Usage patterns and examples

## Related Files

- `src/net/tourbook/importdata/MCPServerManager.java`
- `src/net/tourbook/importdata/MCPServerIntegration.java`
- `src/net/tourbook/importdata/MCPServerUsageExample.java`

## License

This implementation is part of MyTourBook and is licensed under the GNU General Public License v2.0 (GPLv2).
