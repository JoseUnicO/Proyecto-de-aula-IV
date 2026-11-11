from pathlib import Path
import os,subprocess,tempfile,shutil,time,json,datetime
root=Path(__file__).resolve().parents[3]
e=Path(os.environ['BIBLIOTECA_TEST_EVIDENCE']);e.mkdir(parents=True,exist_ok=True)
lab=Path(tempfile.mkdtemp(prefix='biblioteca arranque mysql '))
mysql=r'C:\wamp64\bin\mysql\mysql8.4.7\bin\mysql.exe'
rows=[]
def sql(query):
 p=subprocess.run([mysql,'--user=root','--batch','--skip-column-names','-e',query],capture_output=True,text=True,encoding='utf-8',timeout=10)
 if p.returncode:raise RuntimeError(p.stderr)
 return p.stdout.strip()
def seed(reserva=False):
 sql("USE biblioteca_pruebas;DELETE FROM operaciones;DELETE FROM reservas;DELETE FROM prestamos;DELETE FROM libros;DELETE FROM usuarios;UPDATE secuencias SET siguiente=2;UPDATE administradores SET fallos=0,bloqueado_hasta=NULL;INSERT INTO usuarios VALUES('A','Ana QA','a@example.com',3,0),('B','Bruno QA','b@example.com',3,0);INSERT INTO libros VALUES('L','Libro QA','Autor','9780307474728',2000,'Ficción');INSERT INTO prestamos(id,usuario_id,libro_id,fecha,esperada,estado) VALUES('PREST-00001','A','L',CURRENT_DATE(),CURRENT_DATE()+INTERVAL 14 DAY,'ACTIVO');"+("INSERT INTO reservas(id,usuario_id,libro_id,fecha,orden,estado) VALUES('RES-00001','B','L',CURRENT_DATE(),1,'PENDIENTE');" if reserva else ''))
def record(id,title,ok,detail):
 rows.append(dict(id=id,titulo=title,estado='OK' if ok else 'FALLO',observado=detail));print(id,rows[-1]['estado'],detail,flush=True)
def copia(nombre,resource=True):
 dest=lab/nombre;dest.mkdir();shutil.copytree(root/'src/main',dest/'src/main')
 if not resource:
  # Omitir recursos al crear una segunda copia; no se altera el proyecto original.
  dest=lab/(nombre+' sin recursos');dest.mkdir();shutil.copytree(root/'src/main/java',dest/'src/main/java')
 for n in ['pom.xml','run.bat','Arrancar.ps1']:shutil.copy2(root/n,dest/n)
 return dest
def bat(id,case,args='',gui=False,cwd=None):
 env=os.environ.copy()
 if gui:env['JAVA_TOOL_OPTIONS']='"-javaagent:'+str(root/'target/agente-qa.jar')+'='+str(e)+'"'
 else:env.pop('JAVA_TOOL_OPTIONS',None)
 cmd='cmd.exe /d /s /c ""'+str(case/'run.bat')+'" -Perfil biblioteca_pruebas '+args+'"'
 t=time.monotonic()
 p=subprocess.run(cmd,cwd=cwd or case,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
 (e/(id+'.log')).write_bytes(p.stdout)
 return p.returncode,p.stdout.decode('utf-8',errors='replace'),round(time.monotonic()-t,2)
seed()
for id,title,case,cwd in [('BAT-01','BAT real, login, navegación y cierre',root,root),('BAT-02','BAT desde otro directorio',root,lab),('BAT-06','BAT en copia con espacios',copia('proyecto con espacios'),lab)]:
 code,text,secs=bat(id,case,gui=True,cwd=cwd);record(id,title,code==0 and 'BAT_GUI_OK' in text,f'exit={code}; segundos={secs}; GUI completa={"BAT_GUI_OK" in text}')
case=copia('fallo compilacion');p=case/'src/main/java/aplicacion/Main.java';p.write_text(p.read_text(encoding='utf-8')+'\nESTO_NO_COMPILA\n',encoding='utf-8')
code,text,secs=bat('BAT-03',case);record('BAT-03','Error de compilación detiene BAT',code!=0 and 'no pudo compilar' in text,f'exit={code}')
case=copia('fallo recursos',False);code,text,secs=bat('BAT-04',case);record('BAT-04','Recursos faltantes detienen BAT',code!=0 and 'Recurso requerido ausente' in text,f'exit={code}')
case=copia('fallo java');p=case/'src/main/java/gui/BibliotecaApp.java';p.write_text(p.read_text(encoding='utf-8').replace('public void start(Stage stage){','public void start(Stage stage){if(System.getProperty("java.version")!=null)throw new RuntimeException("FALLO_QA_RUNTIME");'),encoding='utf-8')
code,text,secs=bat('BAT-05',case);record('BAT-05','Error de Java conserva salida no cero',code!=0 and 'FALLO_QA_RUNTIME' in text,f'exit={code}; excepción verificada={"FALLO_QA_RUNTIME" in text}')
# Pruebas de consola: sólo esquema aislado y credenciales de prueba vía entrada estándar.
java=Path(os.environ['JAVA_HOME'])/'bin/java.exe';cp=str(root/'target/classes')+';'+str(root/'target/lib/*')
def consola(id,entrada):
 p=subprocess.run([str(java),'--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-cp',cp,'aplicacion.Main'],cwd=lab,input=entrada.encode(),capture_output=True,timeout=20)
 text=(p.stdout+p.stderr).decode('cp1252' if b'\xe9' in p.stdout else 'utf-8',errors='replace');(e/(id+'.log')).write_text(text,encoding='utf-8');return p.returncode,text
seed();code,text=consola('CON-01','incorrecto\nincorrecto\n'*5);record('CON-01','Credenciales erróneas no acceden',code==0 and 'Acceso denegado' in text and 'MENÚ PRINCIPAL' not in text,f'exit={code}')
login='QA\n'+os.environ['BIBLIOTECA_QA_PASSWORD']+'\n'
seed();code,text=consola('CON-02',login+'0\n');record('CON-02','Login SQL y salida normal',code==0 and 'Sesión cerrada' in text,f'exit={code}')
seed();code,text=consola('CON-03',login+'4\n1\nB\nL\n0\n0\n');count=sql('SELECT COUNT(*) FROM biblioteca_pruebas.reservas');record('CON-03','Reserva de consola persiste en SQL',code==0 and count=='1',f'exit={code}; reservas={count}')
seed(True);code,text=consola('CON-04',login+'4\n4\n0\n0\n');record('CON-04','Consola restaura reservas desde SQL',code==0 and 'RES-00001' in text,f'exit={code}')
seed();code,text=consola('CON-05',login);record('CON-05','EOF cierra sesión de forma controlada',code==0 and 'NoSuchElementException' not in text and 'Fin de entrada' in text,f'exit={code}')
(e/'arranque-consola-resultados.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
print('ARRANQUE_RESUMEN:',sum(r['estado']=='OK' for r in rows),'/',len(rows),flush=True)
raise SystemExit(1 if any(r['estado']!='OK' for r in rows) else 0)
