package model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import model.Event.EventType;
import model.Patient.ColorCode;

public class Simulator {

	
	//coda degli eventi
	private PriorityQueue<Event> queue;
	
	//modello del mondo 
	private List<Patient> patients;
	private PriorityQueue<Patient> waitingRoom;
	//contiene solo i pazienti in attesa -> white, yellow, red 
	
	private int freeStudios; //numero studi liberi
	
	
	//paramentri d input
	private int totStudios=3; //NS 
	
	private int numeroPazienti=120; //NP
	private Duration T_Arrival= Duration.ofMinutes(5);
	
	private Patient.ColorCode ultimoColore;
	
	private Duration Duration_Triage= Duration.ofMinutes(5);
	private Duration Duration_white= Duration.ofMinutes(10);
	private Duration Duration_yello= Duration.ofMinutes(15);
	private Duration Duration_red= Duration.ofMinutes(30);
	
	private Duration Timeout_white= Duration.ofMinutes(60);
	private Duration Timeout_yellow= Duration.ofMinutes(30);
	private Duration Timeout_red= Duration.ofMinutes(30);
	
	private LocalTime startTime= LocalTime.of(8, 00);
	private LocalTime endTime= LocalTime.of(20, 00);
	
	//paramentri di output
	private int patientsTreated;
	private int patientsAbadoned;
	private int patientsDead;
	
	//inizializza il simulatore e crea eventi iniziali 
		public void init() {
			
			//inizializza coda eventi 
			this.queue= new PriorityQueue<>();
			
			//inizializza modello del mondo 
			this.patients= new ArrayList<>();
			this.waitingRoom= new PriorityQueue<>();
			this.freeStudios= this.totStudios;
			this.ultimoColore= ColorCode.RED;
			
			//inizializza i parametri di output 
			this.patientsAbadoned=0;
			this.patientsDead=0;
			this.patientsTreated=0;
			
			//inietta gli eventidi input (arrival)
			LocalTime ora = this.startTime;
			int inseriti=0;
			Patient.ColorCode colore= ColorCode.WHITE;
			this.queue.add(new Event(ora,EventType.TICK,null));
			while(ora.isBefore(endTime) && inseriti<this.numeroPazienti) {
				
				Patient p= new Patient(inseriti,ora,ColorCode.NEW);
				inseriti++;
				
				Event e= new Event(ora,EventType.ARRIVAL, p);
				
				this.queue.add(e);
				this.patients.add(p);
				
				ora=ora.plus(T_Arrival);
				
				/*
				*/
			}
			
		}
		
		private Patient.ColorCode prossimoColore(){
			if(ultimoColore.equals(ColorCode.WHITE))
				ultimoColore= ColorCode.YELLOW;
			else if(ultimoColore.equals(ColorCode.YELLOW))
				ultimoColore=ColorCode.RED;
			else
				ultimoColore= ColorCode.WHITE;
			return ultimoColore; 
		}
		
		//esegue la simualzione
		public void run() {
			while(!this.queue.isEmpty()) {
				Event e= this.queue.poll();
				System.out.println(e);
				processEvent(e);
			}
		}
		
		private void processEvent(Event e) {
			
			Patient p= e.getPatient();
			LocalTime ora= e.getTime();
			
			
			switch(e.getType()) {
			case ARRIVAL:
				this.queue.add(new Event(ora.plus(Duration_Triage),EventType.TRIAGE,p));
				break;
			case TRIAGE:
				p.setColor(prossimoColore());
				if(p.getColor().equals(Patient.ColorCode.WHITE)) {
					this.queue.add(new Event(ora.plus(Timeout_white), EventType.TIMEOUT, p));
					this.waitingRoom.add(p);
				}
				else if(p.getColor().equals(Patient.ColorCode.YELLOW)) {
					this.queue.add(new Event(ora.plus(Timeout_yellow), EventType.TIMEOUT, p));
				this.waitingRoom.add(p);
			}
				else if(p.getColor().equals(Patient.ColorCode.RED)) {
					this.queue.add(new Event(ora.plus(Timeout_red), EventType.TIMEOUT, p));
				this.waitingRoom.add(p);
		}
				break;
			case FREE_STUDIO:
				if(this.freeStudios==0)
					return;
				//quale paziente ha ditirro di entrare
				Patient primo= this.waitingRoom.poll();
				if(primo!=null) {
					//ammetti paziente nello studio
					if(primo.getColor().equals(Patient.ColorCode.WHITE))
					this.queue.add(new Event(ora.plus(Duration_white),EventType.TREATED,primo));
					if(primo.getColor().equals(Patient.ColorCode.YELLOW))
						this.queue.add(new Event(ora.plus(Duration_yello),EventType.TREATED,primo));
					if(primo.getColor().equals(Patient.ColorCode.RED))
						this.queue.add(new Event(ora.plus(Duration_red),EventType.TREATED,primo));
					primo.setColor(Patient.ColorCode.TREATING);
					this.freeStudios--;
					
				}
				
				break;
			case TIMEOUT:
				Patient.ColorCode colore = p.getColor();
				switch(colore) {
				case WHITE:
					this.waitingRoom.remove(p);
					p.setColor(ColorCode.OUT);
					this.patientsAbadoned++;
					break;
					
				case YELLOW:
					this.waitingRoom.remove(p);
					p.setColor(ColorCode.RED);
					this.queue.add(new Event(ora.plus(Timeout_red), EventType.TIMEOUT, p));
					this.waitingRoom.add(p);
					
					break;
					
				case RED:
					this.waitingRoom.remove(p);
					p.setColor(ColorCode.BLACK);
					this.patientsDead++;
					break;
					default: 
						//System.out.print("ERRORE: TIMEOUT CON COLORE: "+colore);
				}
				break;
			case TREATED:
				this.patientsTreated++;
				p.setColor(Patient.ColorCode.OUT);
				this.freeStudios++;
				this.queue.add(new Event(ora,EventType.FREE_STUDIO,null));
				break;
			case TICK:
				if(this.freeStudios>0 && !this.waitingRoom.isEmpty())
					this.queue.add(new Event(ora,EventType.FREE_STUDIO,null));
				if(ora.isBefore(endTime))
				this.queue.add(new Event(ora.plus(Duration.ofMinutes(5)),EventType.TICK,null));
				break;
			}
			
			
		}
	
	
	public int getPatientsTreated() {
			return patientsTreated;
		}

		public int getPatientsAbadoned() {
			return patientsAbadoned;
		}

		public int getPatientsDead() {
			return patientsDead;
		}

	public void setQueue(PriorityQueue<Event> queue) {
		this.queue = queue;
	}

	public void setPatients(List<Patient> patients) {
		this.patients = patients;
	}

	public void setFreeStudios(int freeStudios) {
		this.freeStudios = freeStudios;
	}

	public void setTotStudios(int totStudios) {
		this.totStudios = totStudios;
	}

	public void setNumeroPazienti(int numeroPazienti) {
		this.numeroPazienti = numeroPazienti;
	}

	public void setT_Arrival(Duration t_Arrival) {
		T_Arrival = t_Arrival;
	}

	public void setDuration_Triage(Duration duration_Triage) {
		Duration_Triage = duration_Triage;
	}

	public void setDuration_white(Duration duration_white) {
		Duration_white = duration_white;
	}

	public void setDuration_yello(Duration duration_yello) {
		Duration_yello = duration_yello;
	}

	public void setDuration_red(Duration duration_red) {
		Duration_red = duration_red;
	}

	public void setTimeout_white(Duration timeout_white) {
		Timeout_white = timeout_white;
	}

	public void setTimeout_yellow(Duration timeout_yellow) {
		Timeout_yellow = timeout_yellow;
	}

	public void setTimeout_red(Duration timeout_red) {
		Timeout_red = timeout_red;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public void setPatientsTreated(int patientsTreated) {
		this.patientsTreated = patientsTreated;
	}

	public void setPatientsAbadoned(int patientsAbadoned) {
		this.patientsAbadoned = patientsAbadoned;
	}

	public void setPatientsDead(int patientsDead) {
		this.patientsDead = patientsDead;
	}

	
}
