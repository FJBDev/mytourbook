/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.importdata;

/**
 * MCPServerUsageExample demonstrates how to use the MCP Server Manager and Integration classes
 * to create an MCP server for MyTourBook.
 *
 * This example can be used as a template for integrating MCP server functionality into
 * the application at startup or during runtime.
 */
public class MCPServerUsageExample {

	/**
	 * Example 1: Basic MCP Server initialization and startup
	 */
	public static void example1_BasicServerSetup() {

		System.out.println("=== Example 1: Basic MCP Server Setup ==="); //$NON-NLS-1$

		final MCPServerManager manager = MCPServerManager.getInstance();

		// Initialize the server
		if (!manager.initializeServer()) {
			System.err.println("Failed to initialize MCP server"); //$NON-NLS-1$
			return;
		}

		// Start the server
		if (!manager.startServer()) {
			System.err.println("Failed to start MCP server"); //$NON-NLS-1$
			return;
		}

		System.out.println("MCP Server is running: " + manager.isServerRunning()); //$NON-NLS-1$

		// Stop the server when done
		// manager.stopServer();
	}

	/**
	 * Example 2: Register custom tools
	 */
	public static void example2_RegisterCustomTools() {

		System.out.println("=== Example 2: Register Custom Tools ==="); //$NON-NLS-1$

		final MCPServerIntegration.MCPTool customTool = new MCPServerIntegration.MCPTool(
				"analyze_tour",
				"Analyze a specific tour and provide insights",
				"{\"type\":\"object\",\"properties\":{\"tour_id\":{\"type\":\"string\"},\"analysis_type\":{\"type\":\"string\"}}}");

		if (MCPServerIntegration.registerTool(customTool)) {
			System.out.println("Custom tool registered successfully"); //$NON-NLS-1$
		} else {
			System.err.println("Failed to register custom tool"); //$NON-NLS-1$
		}
	}

	/**
	 * Example 3: Register custom resources
	 */
	public static void example3_RegisterCustomResources() {

		System.out.println("=== Example 3: Register Custom Resources ==="); //$NON-NLS-1$

		final MCPServerIntegration.MCPResource customResource = new MCPServerIntegration.MCPResource(
				"mytourbook://gps-data",
				"application/json",
				"GPS data from imported tours");

		if (MCPServerIntegration.registerResource(customResource)) {
			System.out.println("Custom resource registered successfully"); //$NON-NLS-1$
		} else {
			System.err.println("Failed to register custom resource"); //$NON-NLS-1$
		}
	}

	/**
	 * Example 4: List all registered tools and resources
	 */
	public static void example4_ListAllToolsAndResources() {

		System.out.println("=== Example 4: List All Tools and Resources ==="); //$NON-NLS-1$

		System.out.println("\nRegistered Tools:"); //$NON-NLS-1$
		MCPServerIntegration.getAllTools().forEach((name, tool) -> {
			System.out.println("  - " + tool.name + ": " + tool.description); //$NON-NLS-1$
		});

		System.out.println("\nRegistered Resources:"); //$NON-NLS-1$
		MCPServerIntegration.getAllResources().forEach((uri, resource) -> {
			System.out.println("  - " + resource.uri + " (" + resource.mimeType + "): " + resource.description); //$NON-NLS-1$ //$NON-NLS-2$
		});
	}

	/**
	 * Example 5: Complete server lifecycle management
	 */
	public static void example5_CompleteLifecycle() {

		System.out.println("=== Example 5: Complete Server Lifecycle ==="); //$NON-NLS-1$

		final MCPServerManager manager = MCPServerManager.getInstance();

		try {

			// Step 1: Initialize
			System.out.println("Step 1: Initializing MCP server..."); //$NON-NLS-1$
			if (!manager.initializeServer()) {
				System.err.println("Initialization failed"); //$NON-NLS-1$
				return;
			}

			// Step 2: Register tools
			System.out.println("Step 2: Registering tools..."); //$NON-NLS-1$
			MCPServerIntegration.initializeDefaultTools();

			// Step 3: Start server
			System.out.println("Step 3: Starting MCP server..."); //$NON-NLS-1$
			if (!manager.startServer()) {
				System.err.println("Start failed"); //$NON-NLS-1$
				return;
			}

			// Step 4: Verify running
			System.out.println("Step 4: Verifying server status..."); //$NON-NLS-1$
			System.out.println("Server running: " + manager.isServerRunning()); //$NON-NLS-1$

			// Step 5: Simulate some operations
			System.out.println("Step 5: Server is ready for operations..."); //$NON-NLS-1$
			Thread.sleep(2000);

			// Step 6: Gracefully shutdown
			System.out.println("Step 6: Stopping MCP server..."); //$NON-NLS-1$
			if (manager.stopServer()) {
				System.out.println("Server stopped successfully"); //$NON-NLS-1$
			}

		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("Lifecycle interrupted: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	/**
	 * Example 6: Server restart
	 */
	public static void example6_ServerRestart() {

		System.out.println("=== Example 6: Server Restart ==="); //$NON-NLS-1$

		final MCPServerManager manager = MCPServerManager.getInstance();

		// Initialize first
		if (!manager.initializeServer()) {
			System.err.println("Initialization failed"); //$NON-NLS-1$
			return;
		}

		// Start server
		if (!manager.startServer()) {
			System.err.println("Start failed"); //$NON-NLS-1$
			return;
		}

		System.out.println("Server started: " + manager.isServerRunning()); //$NON-NLS-1$

		// Restart the server
		if (manager.restartServer()) {
			System.out.println("Server restarted successfully"); //$NON-NLS-1$
		} else {
			System.err.println("Server restart failed"); //$NON-NLS-1$
		}

		manager.stopServer();
	}

	/**
	 * Main method to run all examples
	 *
	 * @param args command line arguments (not used)
	 */
	public static void main(final String[] args) {

		System.out.println("MyTourBook MCP Server Usage Examples\n"); //$NON-NLS-1$

		try {

			// Run all examples
			example1_BasicServerSetup();
			System.out.println();

			example2_RegisterCustomTools();
			System.out.println();

			example3_RegisterCustomResources();
			System.out.println();

			example4_ListAllToolsAndResources();
			System.out.println();

			example5_CompleteLifecycle();
			System.out.println();

			example6_ServerRestart();
			System.out.println();

			System.out.println("\n=== All examples completed ==="); //$NON-NLS-1$

		} catch (final Exception e) {

			System.err.println("Error running examples: " + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
		}
	}
}
