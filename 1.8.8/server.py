import subprocess
import threading
import time
import os
import sys

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def read_server_output(process):
    """读取并打印服务器输出"""
    for line in iter(process.stdout.readline, b''):
        try:
            # 尝试多种编码解码输出
            try:
                text = line.decode('utf-8')
            except UnicodeDecodeError:
                try:
                    text = line.decode('gbk')
                except UnicodeDecodeError:
                    text = line.decode('utf-8', errors='replace')
            print(text, end='')
        except Exception as e:
            print(f"读取输出错误: {e}")

def start_server():
    """启动并监控Minecraft服务器"""
    while True:
        clear_screen()
        print("=== Minecraft Paper 1.8.8 服务器 ===")
        print("输入\"restart\"重启服务器")
        print("输入\"exit\"关闭服务器并退出")
        print("====================================")
        
        # 启动Minecraft服务器
        process = subprocess.Popen(
            ["java", "-Xms2048M", "-Xmx4096M", "-jar", "paper.jar", "nogui"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=False,
            bufsize=1
        )
        
        # 启动线程读取服务器输出
        output_thread = threading.Thread(target=read_server_output, args=(process,))
        output_thread.daemon = True
        output_thread.start()
        
        # 监听用户输入
        restart = False
        while process.poll() is None:
            try:
                command = input()
                if command.lower() == "restart":
                    print("正在重启服务器...")
                    process.stdin.write("stop\n".encode())
                    process.stdin.flush()
                    print("等待服务器保存数据 (5秒)...")
                    time.sleep(5)  # 等待5秒保存数据
                    
                    # 检查服务器是否已关闭
                    if process.poll() is None:
                        print("服务器仍在运行，强制终止...")
                        process.terminate()
                        time.sleep(2)
                        if process.poll() is None:
                            process.kill()
                    restart = True
                    break
                elif command.lower() == "exit":
                    print("正在停止服务器...")
                    process.stdin.write("stop\n".encode())
                    process.stdin.flush()
                    print("等待服务器保存数据 (5秒)...")
                    time.sleep(5)  # 等待5秒保存数据
                    
                    # 检查服务器是否已关闭
                    if process.poll() is None:
                        print("服务器仍在运行，强制终止...")
                        process.terminate()
                        time.sleep(2)
                        if process.poll() is None:
                            process.kill()
                    restart = False
                    break
                else:
                    # 将命令发送到服务器
                    process.stdin.write(f"{command}\n".encode())
                    process.stdin.flush()
            except EOFError:
                break
            except Exception as e:
                print(f"输入错误: {e}")
                
        # 等待服务器完全关闭
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            print("服务器未能在超时时间内关闭，强制终止...")
            process.kill()
        
        # 如果不是重启，退出循环
        if not restart:
            print("服务器已关闭，正在退出...")
            break
        
        print("准备重启服务器...")
        time.sleep(1)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n捕获到Ctrl+C，正在退出...")
    except Exception as e:
        print(f"发生错误: {e}")
    finally:
        print("脚本已退出。")