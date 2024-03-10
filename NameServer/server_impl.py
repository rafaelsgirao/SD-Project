import threading
from grpc import StatusCode
import NameServer_pb2_grpc as pb2_grpc
import NameServer_pb2 as pb2
import sys
from debug import debug

sys.path.insert(1, "../contract/target/generated-sources/protobuf/python")


class ServerEntry:
    def __init__(self, address, qualifier):
        self.address = address  # -> host:port
        self.qualifier = qualifier  # A, B or C


class ServiceEntry:
    def __init__(self, name, server_entries):
        self.service_name = name
        self.server_entries = server_entries


class NamingServer:
    def __init__(self):
        self.lock = threading.Lock()  # lock for the service list
        self.services = []  # list of Service Entries


class NamingServerServiceImpl(pb2_grpc.NameServerServiceServicer):
    def __init__(self, *args, **kwargs):
        self.namingServer = NamingServer()

    def register(self, request, context):
        debug(request)  # received request

        name = request.name
        qualifier = request.qualifier
        address = request.address

        server = ServerEntry(address, qualifier)

        self.namingServer.lock.acquire()
        for entry in self.namingServer.services:
            # check if the service name is already registered
            if entry.service_name == name:
                # service already registered
                # check if the server is already registered
                if qualifier in entry.server_entries:
                    # server found -> return error (server already registered)
                    print(
                        f"Server {name} @ {address}, qualifier {qualifier} already registered!"
                    )
                    context.set_code(StatusCode.ALREADY_EXISTS)
                    context.set_details("Not possible to register the server")
                    self.namingServer.lock.release()
                    return
                else:
                    # server not found -> add server
                    entry.server_entries.append(server)
                    print(
                        f"Server {name} @ {address}, qualifier {qualifier} registered!"
                    )

                    self.namingServer.lock.release()
                    return pb2.RegisterResponse()

        # no service found -> create new service
        service = ServiceEntry(name, [server])
        print(f"Server {name} @ {address}, qualifier {qualifier} registered!")
        self.namingServer.services.append(service)
        self.namingServer.lock.release()
        return pb2.RegisterResponse()

    def lookup(self, request, context):
        debug(request)  # received request

        name = request.name
        qualifier = request.qualifier
        result = []

        self.namingServer.lock.acquire()
        for entry in self.namingServer.services:
            # check if the service name is already registered
            if entry.service_name == name:
                # service found
                for server in entry.server_entries:
                    # check if the qualifier is already registered
                    if server.qualifier == qualifier:
                        # qualifier found -> return the address
                        result.append(server.address)

                # if there are results, return them
                if len(result) > 0:
                    for r in result:
                        print(f"Qualifier {qualifier} found for server {name} @ {r}")

                    self.namingServer.lock.release()
                    return pb2.LookupResponse(result=result)

                # if there are no qualifiers, return all addresses
                else:
                    server_addresses = [
                        server.address for server in entry.server_entries
                    ]
                    print(f"Qualifier {qualifier} not found for server {name}")

                    self.namingServer.lock.release()
                    return pb2.LookupResponse(result=server_addresses)

        # neither qualifier nor service found
        print(f"Server {name}, qualifier {qualifier} not found!")

        self.namingServer.lock.release()
        return pb2.LookupResponse(result=[])

    def delete(self, request, context):
        debug(request)  # received request

        name = request.name
        address = request.address

        self.namingServer.lock.acquire()
        for entry in self.namingServer.services:
            # check if the service name is already registered
            if entry.service_name == name:
                # service found
                for server in entry.server_entries:
                    # check if the address is already registered
                    if server.address == address:
                        # address found -> remove the server
                        entry.server_entries.remove(server)
                        print(
                            f"Server {name} @ {address}, qualifier {server.qualifier} deleted!"
                        )
                        self.namingServer.lock.release()
                        return pb2.DeleteResponse()

        # neither service nor address found
        print(f"Server {name} @ {address} not found!")
        context.set_code(StatusCode.NOT_FOUND)
        context.set_details("Not possible to remove the server")
        self.namingServer.lock.release()
