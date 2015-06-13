package net.marcomichel.ed.converter;

public class Edge {
	private String quelle;
	private String ziel;

	public Edge(String quelle, String ziel) {
		super();
		this.quelle = quelle;
		this.ziel = ziel;
	}
	
	public String getQuelle() {
		return quelle;
	}
	public void setQuelle(String quelle) {
		this.quelle = quelle;
	}
	public String getZiel() {
		return ziel;
	}
	public void setZiel(String ziel) {
		this.ziel = ziel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((quelle == null) ? 0 : quelle.hashCode());
		result = prime * result + ((ziel == null) ? 0 : ziel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (quelle == null) {
			if (other.quelle != null)
				return false;
		} else if (!quelle.equals(other.quelle))
			return false;
		if (ziel == null) {
			if (other.ziel != null)
				return false;
		} else if (!ziel.equals(other.ziel))
			return false;
		return true;
	}
	
}
