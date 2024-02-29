import sys
sys.path.insert(1, '../contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from grpc import StatusCode

class ServerEntry:
	def __init__(self, address, qualifier):
		self.address = address   # -> host:port
		self.qualifier = qualifier # A, B or C

class ServiceEntry:
	def __init__(self, name, server_entries):
		self.serviceName = name
		self.server_entries = server_entries

class NamingServer:
  services = []  # list of Service Entries

class NamingServerServiceImpl(pb2_grpc.NameServerServiceServicer):
	def __init__(self, *args, **kwargs):
			self.namingServer = NamingServer()
			pass

	def register(self, request, context):
			print(request) # received request

			name = request.name
			qualifier = request.qualifier
			address = request.address
			
			server = ServerEntry(address, qualifier)
			# check  if the name is already registered with that server
			for entry in self.namingServer.services:
					if entry.serviceName == name:
							# service already registered
							if (qualifier in entry.server_entries):
								return pb2.RegisterResponse(result='Service already registered')
							else:
								entry.server_entries.append(server)
								return pb2.RegisterResponse(result='')
			
			# no service found -> create new service
			service = ServiceEntry(name, [server])
			self.namingServer.services.append(service)
			
			return pb2.RegisterResponse(result='')
		
	def lookup(self, request, context):
		print(request) # received request

		name = request.name
		qualifier = request.qualifier
		result = []
		# check if the name is registered
		for entry in self.namingServer.services:
			if entry.serviceName == name:
				# service found
				for server in entry.server_entries:
					# qualifier found
					if server.qualifier == qualifier:
						result.append(server.address)
			
				if (len(result) > 0):
					return pb2.LookupResponse(result=result)
				else:
					# qualifier not found
					# get all server addresses from server entries
					server_addresses = [server.address for server in entry.server_entries]
					return pb2.LookupResponse(result=server_addresses)
			
		# neither qualifier nor service found
		return pb2.LookupResponse(result=[])

	def delete(self, request, context):
		print(request) # received request

		name = request.name
		address = request.address
		
		for entry in self.namingServer.services:
			if entry.serviceName == name:
				# service found
				for server in entry.server_entries:
					# qualifier found
					if server.address == address:
						entry.server_entries.remove(server)
						print(f"Server {name} @ {address} deleted!")
						return pb2.DeleteResponse()
		
		# neither qualifier nor service found

		print(f"Server {name} @ {address} not found!")
		context.set_code(StatusCode.NOT_FOUND)
		context.set_details('Service not found.')
		return
		#return StatusCode.NOT_FOUND("Service not found")
    
      
      