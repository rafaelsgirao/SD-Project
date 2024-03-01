import sys

sys.path.insert(1, "../Contract/target/generated-sources/protobuf/python")
import grpc
import NameServer_pb2_grpc as pb2_grpc
from NamingServerServiceImpl import NamingServerServiceImpl
from concurrent import futures
from debug import set_debug
from debug import debug
from debug import is_debug_enable

# define the port
PORT = 5001

if __name__ == "__main__":
    try:
        for i in range(1, len(sys.argv)):
            if sys.argv[i] == "-debug":
                set_debug(True)
                debug("Debug mode enabled")
        if is_debug_enable():
            for i in range(1, len(sys.argv)):
                debug("Argument: " + sys.argv[i])

        # get port
        port = int(sys.argv[1])

        # create server
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        # add service
        pb2_grpc.add_NameServerServiceServicer_to_server(
            NamingServerServiceImpl(), server
        )
        # listen on port
        server.add_insecure_port("[::]:" + str(port))
        # start server
        server.start()
        # print message
        print("Server listening on port " + str(port))
        # print termination message
        print("Press CTRL+C to terminate\n")
        # wait for server to finish
        server.wait_for_termination()

    except KeyboardInterrupt:
        print("NameServer stopped")
        exit(0)
