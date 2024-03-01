DEBUG_FLAG = False


def debug(message):
    if DEBUG_FLAG:
        print(f"\033[1;32;40mDEBUG: {message}\033[m")


def set_debug(flag):
    global DEBUG_FLAG
    DEBUG_FLAG = flag
    return DEBUG_FLAG


def is_debug_enable():
    return DEBUG_FLAG
